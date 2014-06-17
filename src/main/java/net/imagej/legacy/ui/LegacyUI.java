/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2014 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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

import ij.io.OpenDialog;
import ij.io.SaveDialog;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.JFileChooser;

import net.imagej.legacy.DefaultLegacyService;
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
import org.scijava.ui.awt.AWTDropTargetEventDispatcher;
import org.scijava.ui.awt.AWTInputEventDispatcher;
import org.scijava.ui.awt.AWTWindowEventDispatcher;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.swing.viewer.SwingDisplayWindow;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.util.FileUtils;
import org.scijava.widget.FileWidget;

/**
 * Swing-based {@link UserInterface} implementation for working between
 * ImageJ2 ({@code net.imagej.*}) and ImageJ1 ({@code ij.*}) applications.
 *
 * @author Johannes Schindelin
 * @author Mark Hiner
 */
@Plugin(type = UserInterface.class, name = LegacyUI.NAME,
	priority = Priority.HIGH_PRIORITY)
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

	private SystemClipboard systemClipboard;

	private IJ1Helper ij1Helper() {
		// FIXME: See https://github.com/imagej/imagej-legacy/issues/53
		if (legacyService instanceof DefaultLegacyService) {
			ij1Helper = ((DefaultLegacyService) legacyService).getIJ1Helper();
		}
		return ij1Helper;
	}

	@Override
	public void dispose() {
		if (ij1Helper() != null) ij1Helper.dispose();
	}

	@Override
	public void show() {
		if (ij1Helper() != null) {
			ij1Helper.setVisible(true);
		}
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
						finalViewer.view(null, display);
					}
				});
			}
			catch (InterruptedException e) {
				legacyService.handleException(e);
			}
			catch (InvocationTargetException e) {
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
	public synchronized SystemClipboard getSystemClipboard() {
		// TODO consider extending abstractAWTUI common class..
		if (systemClipboard != null) return systemClipboard;
		systemClipboard = new AWTClipboard();
		return systemClipboard;
	}

	@Override
	public DisplayWindow createDisplayWindow(final Display<?> display) {
		final SwingDisplayWindow displayWindow = new SwingDisplayWindow();

		// broadcast input events (keyboard and mouse)
		new AWTInputEventDispatcher(display).register(displayWindow, true, false);

		// broadcast window events
		new AWTWindowEventDispatcher(display).register(displayWindow);

		// broadcast drag-and-drop events
		new AWTDropTargetEventDispatcher(display, eventService);

		return displayWindow;

		//FIXME: replace with this code after releasing scijava-ui-swing
//		return SwingSDIUI.createDisplayWindow(display, eventService);
	}

	@Override
	public DialogPrompt dialogPrompt(final String message, final String title,
		final MessageType messageType, final OptionType optionType)
	{
		return new LegacyDialogPrompt(legacyService, message, title, optionType);
	}

	@Override
	public File chooseFile(final File file, final String style) {
		final File[] chosenFile = new File[1];

		// Run on the EDT to avoid deadlocks, per ij.io.Opener
		try {
			threadService.invoke(new Runnable() {

				@Override
				public void run() {
					if (FileWidget.DIRECTORY_STYLE.equals(style)) {
						// ImageJ1 does not provide a directory chooser, so we use a
						// Swing JFileChooser in "DIRECTORIES_ONLY" mode.
						final JFileChooser chooser = new JFileChooser(file);
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						final int rval =
							chooser.showOpenDialog(getApplicationFrame().getComponent());
						if (rval == JFileChooser.APPROVE_OPTION) {
							chosenFile[0] = chooser.getSelectedFile();
						}
					}
					else if (FileWidget.SAVE_STYLE.equals(style)) {
						// Use ImageJ1's SaveDialog.
						final String defaultName = file.getName();
						final String extension = FileUtils.getExtension(file);
						final SaveDialog saveDialog =
							new SaveDialog("Save", defaultName, extension);
						final String directory = saveDialog.getDirectory();
						final String fileName = saveDialog.getFileName();
						if (directory != null && fileName != null) {
							chosenFile[0] = new File(directory, fileName);
						}
					}
					else { // FileWidget.OPEN_STYLE / default behavior
						// Use ImageJ1's OpenDialog.
						final OpenDialog openDialog = new OpenDialog("Open");
						final String directory = openDialog.getDirectory();
						final String fileName = openDialog.getFileName();
						if (directory != null && fileName != null) {
							chosenFile[0] = new File(directory, fileName);
						}
					}
				}
			});
		}
		catch (InterruptedException e) {
			legacyService.handleException(e);
		}
		catch (InvocationTargetException e) {
			legacyService.handleException(e);
		}
		return chosenFile[0];
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
}
