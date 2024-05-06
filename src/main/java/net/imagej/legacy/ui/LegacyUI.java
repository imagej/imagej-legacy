/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2024 ImageJ2 developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.legacy.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;
import net.imagej.legacy.display.LegacyDisplayViewer;

import org.scijava.Priority;
import org.scijava.display.Display;
import org.scijava.event.EventService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.AbstractUserInterface;
import org.scijava.ui.Desktop;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.DialogPrompt.OptionType;
import org.scijava.ui.StatusBar;
import org.scijava.ui.SystemClipboard;
import org.scijava.ui.ToolBar;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.ui.awt.AWTClipboard;
import org.scijava.ui.console.ConsolePane;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.swing.console.SwingConsolePane;
import org.scijava.ui.swing.sdi.SwingSDIUI;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.widget.FileListWidget;
import org.scijava.widget.FileWidget;

/**
 * Swing-based {@link UserInterface} implementation for working between
 * ImageJ2 ({@code net.imagej.*}) and ImageJ1 ({@code ij.*}) applications.
 *
 * @author Johannes Schindelin
 * @author Mark Hiner
 */
@Plugin(type = UserInterface.class, name = LegacyUI.NAME,
	priority = Priority.HIGH)
public class LegacyUI extends AbstractUserInterface implements SwingUI {

	public static final String NAME = "legacy";

	@Parameter
	private LegacyService legacyService;

	@Parameter
	private UIService uiService;

	@Parameter
	private LogService log;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private ThreadService threadService;

	@Parameter
	private EventService eventService;

	private IJ1Helper ij1Helper;

	private LegacyApplicationFrame applicationFrame;
	private ToolBar toolBar;
	private StatusBar statusBar;
	private SwingConsolePane consolePane;
	private SystemClipboard systemClipboard;

	private IJ1Helper ij1Helper() {
		ij1Helper = legacyService.getIJ1Helper();
		return ij1Helper;
	}

	@Override
	public void dispose() {
		if (ij1Helper() != null) ij1Helper.dispose();
	}

	@Override
	public void show() {
		if (ij1Helper() == null) return;

		ij1Helper.setVisible(true);
		if (ij1Helper.isVisible()) {
			// NB: This check avoids creating a console UI while in headless mode.
			//
			// The ImageJ 1.x headless mode works by pretending to show the UI
			// (i.e., walking through very similar code paths), but without
			// actually instantiating or showing any UI components.
			//
			// So, even though we write ij1Helper.setVisible(true) above, the
			// ImageJ1 user interface will not actually be shown when running
			// in headless mode, and ij1Helper.isVisible() will return false.
			createConsole();
		}
	}

	private synchronized void createConsole() {
		if (consolePane != null) return;
		if (Boolean.getBoolean(ConsolePane.NO_CONSOLE_PROPERTY)) return;
		consolePane = new SwingConsolePane(getContext());

		final JFrame frame = new JFrame("Console");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setContentPane(getConsolePane().getComponent());
		frame.setJMenuBar(createConsoleMenu());
		frame.pack();
		getConsolePane().setWindow(frame);
	}

	@Override
	public void show(final Display<?> display) {
		if (uiService.getDisplayViewer(display) != null) {
			// display is already being shown
			return;
		}

		final List<PluginInfo<DisplayViewer<?>>> viewers =
			uiService.getViewerPlugins();

		DisplayViewer<?> displayViewer = null;
		for (final PluginInfo<DisplayViewer<?>> info : viewers) {
			// check that viewer can actually handle the given display
			final DisplayViewer<?> viewer = pluginService.createInstance(info);
			if (viewer == null) continue;
			if (!viewer.canView(display)) continue;
			if (!viewer.isCompatible(this)) continue;
			displayViewer = viewer;
			break; // found a suitable viewer; we are done
		}
		if (displayViewer == null) {
			log.warn("For UI '" + getClass().getName() +
				"': no suitable viewer for display: " + display);
			return;
		}

		// LegacyDisplayViewers will display through ImagePlus.show, so they don't
		// need DisplayWindows and extra processing that other viewers might..
		if (LegacyDisplayViewer.class.isAssignableFrom(displayViewer.getClass())) {
			final DisplayViewer<?> finalViewer = displayViewer;
			try {
				threadService.invoke(new Runnable() {

					@Override
					public void run() {
						finalViewer.view(LegacyUI.this, display);
					}
				});
			}
			catch (final InterruptedException e) {
				legacyService.handleException(e);
			}
			catch (final InvocationTargetException e) {
				legacyService.handleException(e);
			}
		}
		else {
			super.show(display);
		}
	}

	@Override
	public boolean isVisible() {
		return ij1Helper() != null && ij1Helper.isVisible();
	}

	@Override
	public Desktop getDesktop() {
		return null;
	}

	@Override
	public synchronized LegacyApplicationFrame getApplicationFrame() {
		if (applicationFrame == null) {
			applicationFrame = new LegacyApplicationFrame(legacyService);
		}
		return applicationFrame;
	}

	@Override
	public synchronized ToolBar getToolBar() {
		if (toolBar == null) {
			toolBar = new LegacyToolBar(legacyService);
		}
		return toolBar;
	}

	@Override
	public synchronized StatusBar getStatusBar() {
		if (statusBar == null) {
			statusBar = new LegacyStatusBar(legacyService);
		}
		return statusBar;
	}

	@Override
	public SwingConsolePane getConsolePane() {
		return consolePane;
	}

	@Override
	public synchronized SystemClipboard getSystemClipboard() {
		// TODO consider extending abstractAWTUI common class..
		if (systemClipboard != null) return systemClipboard;
		systemClipboard = new AWTClipboard();
		return systemClipboard;
	}

	@Override
	public DisplayWindow createDisplayWindow(final Display<?> display) {
		return SwingSDIUI.createDisplayWindow(display, eventService);
	}

	@Override
	public DialogPrompt dialogPrompt(final String message, final String title,
		final MessageType messageType, final OptionType optionType)
	{
		return new LegacyDialogPrompt(legacyService, message, title, optionType);
	}

	@Override
	public File chooseFile(final String title, final File file, final String style) {
		final File[] chosenFile = new File[1];

		// Run on the EDT to avoid deadlocks, per ij.io.Opener
		try {
			threadService.invoke(new Runnable() {

				@Override
				public void run() {
					if (FileWidget.DIRECTORY_STYLE.equals(style)) {
						// Use ImageJ1's DirectoryChooser.
						final String dir = ij1Helper().getDirectory(title, file);
						if (dir != null) {
							chosenFile[0] = new File(dir);
						}
					}
					else if (FileWidget.SAVE_STYLE.equals(style)) {
						// Use ImageJ1's SaveDialog.
						final File chosen = ij1Helper().saveDialog(title, file, null);
						if (chosen != null) {
							chosenFile[0] = chosen;
						}
					}
					else { // FileWidget.OPEN_STYLE / default behavior
						// Use ImageJ1's OpenDialog.
						final File chosen = ij1Helper().openDialog(title, file);
						if (chosen != null) {
							chosenFile[0] = chosen;
						}
					}
				}
			});
		}
		catch (final InterruptedException e) {
			legacyService.handleException(e);
		}
		catch (final InvocationTargetException e) {
			legacyService.handleException(e);
		}
		return chosenFile[0];
	}

	@Override
	public File[] chooseFiles(final File parent, final File[] files, final FileFilter filter, final String style) {
		final File[][] result = new File[1][];
		try {
			// NB: Show JFileChooser on the EDT to avoid deadlocks
			threadService.invoke(() -> {
				final JFileChooser chooser = new JFileChooser(parent);
				chooser.setMultiSelectionEnabled(true);
				if (style != null && style.equals(FileListWidget.FILES_AND_DIRECTORIES)) {
					chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				}
				else if (style != null && style.equals(FileListWidget.DIRECTORIES_ONLY)) {
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				}
				else {
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				}
				chooser.setSelectedFiles(files);
				// add the possibility to filter by images and for common spreadsheet formats:
				//chooser.addChoosableFileFilter(new FileNameExtensionFilter("All supported formats", formatService.getSuffixes()));
				//chooser.addChoosableFileFilter(new FileNameExtensionFilter("Table data", "csv", "tsv", "xls", " xlsx", "txt"));
				if (filter != null) {
					javax.swing.filechooser.FileFilter fileFilter = new javax.swing.filechooser.FileFilter() {

						@Override
						public String getDescription() {
							// return filter.toString();
							return "Custom filter"; // TODO get better description
						}

						@Override
						public boolean accept(File f) {
							if (filter.accept(f)) return true;
							// directories should always be displayed
							// independent from selection mode
							return f.isDirectory();
						}
					};
					chooser.setFileFilter(fileFilter);
					chooser.setAcceptAllFileFilterUsed(false);
				}
				int rval = chooser.showOpenDialog(ij1Helper().getIJ());
				if (rval == JFileChooser.APPROVE_OPTION) {
					result[0] = chooser.getSelectedFiles();
				}
			});
		}
		catch (final InvocationTargetException | InterruptedException exc) {
			legacyService.handleException(exc);
		}
		return result[0];
	}

	@Override
	public void showContextMenu(final String menuRoot, final Display<?> display,
		final int x, final int y)
	{
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public void saveLocation() {
		// let ImageJ 1.x do its own thing
	}

	@Override
	public void restoreLocation() {
		// let ImageJ 1.x do its own thing
	}

	@Override
	public boolean requiresEDT() {
		return true;
	}

	// -- Helper methods --

	private JMenuBar createConsoleMenu() {
		final JMenuBar menuBar = new JMenuBar();
		final JMenu edit = new JMenu("Edit");
		menuBar.add(edit);
		final JMenuItem editClear = new JMenuItem("Clear");
		editClear.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				getConsolePane().clear();
			}
		});
		edit.add(editClear);
		return menuBar;
	}

}
