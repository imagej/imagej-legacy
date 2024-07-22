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

package net.imagej.legacy;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.KeyStroke;

import net.imagej.display.ImageDisplay;
import net.imagej.legacy.plugin.LegacyAppConfiguration;
import net.imagej.legacy.plugin.LegacyEditor;
import net.imagej.legacy.plugin.LegacyOpener;
import net.imagej.legacy.plugin.LegacyPostRefreshMenus;
import net.imagej.patcher.LegacyHooks;

import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.log.LogService;
import org.scijava.log.StderrLogService;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.thread.ThreadService;
import org.scijava.ui.CloseConfirmable;
import org.scijava.util.ListUtils;

import ij.ImagePlus;

/**
 * The {@link LegacyHooks} encapsulating an active {@link LegacyService} for use
 * within the patched ImageJ 1.x.
 *
 * @author Johannes Schindelin
 */
public class DefaultLegacyHooks extends LegacyHooks {

	private final LegacyService legacyService;

	private LogService log;
	private LegacyEditor editor;
	private LegacyAppConfiguration appConfig;
	private List<LegacyPostRefreshMenus> afterRefreshMenus;
	private List<LegacyOpener> legacyOpeners;

	/** If the ij.log.file property is set, logs every message to this file. */
	private BufferedWriter logFileWriter;

	public DefaultLegacyHooks(final LegacyService legacyService) {
		this.legacyService = legacyService;
	}

	@Override
	public boolean isLegacyMode() {
		return legacyService.isLegacyMode();
	}

	@Override
	public Object getContext() {
		return legacyService.getContext();
	}

	@Override
	public synchronized void installed() {
		final Context context = legacyService.getContext();
		IJ1Helper.subscribeEvents(context);

		log = context.getService(LogService.class);
		if (log == null) log = new StderrLogService();

		editor = createInstanceOfType(LegacyEditor.class);
		appConfig = createInstanceOfType(LegacyAppConfiguration.class);

		final PluginService pluginService = pluginService();
		afterRefreshMenus =
			pluginService.createInstancesOfType(LegacyPostRefreshMenus.class);
		legacyOpeners = pluginService.createInstancesOfType(LegacyOpener.class);
	}

	@Override
	public void dispose() {
		IJ1Helper.subscribeEvents(null);
		// TODO: if there are still things open, we should object.
	}

	@Override
	public Object interceptRunPlugIn(final String className, final String arg) {
		if (LegacyService.class.getName().equals(className)) return legacyService;
		if (Context.class.getName().equals(className)) {
			return legacyService == null ? null : legacyService.getContext();
		}

		IJ1Helper helper = helper();

		// Intercept IJ1 commands
		if (helper != null) {
			// intercept ij.plugins.Commands
			if (helper.commandsName().equals(className)) {
				if (arg.equals("open")) {
					final Object o = interceptFileOpen(null);
					if (o != null) {
						if (o instanceof String) {
							helper.openPathDirectly((String) o);
						}
						return o;
					}
				}
			}
		}
		final Object legacyCompatibleCommand =
			legacyService.runLegacyCompatibleCommand(className);
		if (legacyCompatibleCommand != null) return legacyCompatibleCommand;

		return null;
	}

	@Override
	public void registerImage(final Object o) {
		if (!legacyService.isSyncEnabled()) return;

		final ImagePlus image = (ImagePlus) o;
		if (image == null) return;
		if (!image.isProcessor()) return;
		if (image.getWindow() == null) return;
		legacyService.log().debug("register legacy image: " + image);
		try {
			legacyService.getImageMap().registerLegacyImage(image);
		}
		catch (final UnsupportedOperationException e) {
			// ignore: the dummy legacy service does not have an image map
		}
	}

	@Override
	public void unregisterImage(final Object o) {
		final ImagePlus image = (ImagePlus) o;
		if (image == null) return;
		legacyService.log().debug("unregister legacy image: " + image);
		try {
			final ImageDisplay disp =
				legacyService.getImageMap().lookupDisplay(image);
			legacyService.getImageMap().unregisterLegacyImage(image, true);
			if (disp != null) {
				disp.close();
			}
		}
		catch (final UnsupportedOperationException e) {
			// ignore: the dummy legacy service does not have an image map
		}
		// end alternate
	}

	@Override
	public void debug(final String string) {
		legacyService.log().debug(string);
	}

	@Override
	public void error(final Throwable t) {
		legacyService.log().error(t);
	}

	@Override
	public void log(final String message) {
		if (message != null) {
			final String logFilePath = System.getProperty("ij.log.file");
			if (logFilePath != null) {
				try {
					if (logFileWriter == null) {
						final OutputStream out = new FileOutputStream(logFilePath, true);
						final Writer writer = new OutputStreamWriter(out, "UTF-8");
						logFileWriter = new java.io.BufferedWriter(writer);
						logFileWriter.write("Started new log on " + new Date() + "\n");
					}
					logFileWriter.write(message);
					if (!message.endsWith("\n")) logFileWriter.newLine();
					logFileWriter.flush();
				}
				catch (final Throwable t) {
					t.printStackTrace();
					System.getProperties().remove("ij.log.file");
					logFileWriter = null;
				}
			}
		}
	}

	/**
	 * Returns the application name for use with ImageJ 1.x.
	 *
	 * @return the application name
	 */
	@Override
	public String getAppName() {
		return appConfig == null ? "ImageJ" : appConfig.getAppName();
	}

	/**
	 * Returns the application version to display in the ImageJ 1.x status bar.
	 *
	 * @return the application version
	 */
	@Override
	public String getAppVersion() {
		final AppService appService = legacyService.getContext().getService(
			AppService.class);
		if (appService == null) return legacyService.getVersion();
		return appService.getApp().getVersion();
	}

	/**
	 * Returns the icon for use with ImageJ 1.x.
	 *
	 * @return the application name
	 */
	@Override
	public URL getIconURL() {
		return appConfig == null ? getClass().getResource("/icons/imagej-256.png")
			: appConfig.getIconURL();
	}

	@Override
	public void runAfterRefreshMenus() {
		if (afterRefreshMenus != null) {
			for (final Runnable run : afterRefreshMenus) {
				run.run();
			}
		}
	}

	/**
	 * Opens the given path in the registered legacy editor, if any.
	 *
	 * @param path the path of the file to open
	 * @return whether the file was opened successfully
	 */
	@Override
	public boolean openInEditor(final String path) {
		if (editor == null) return false;
		if (path.indexOf("://") > 0) return false;
		// if it has no extension, do not open it in the legacy editor
		if (!path.matches(".*\\.[0-9A-Za-z]+")) return false;
		if (stackTraceContains(getClass().getName() + ".openInEditor(")) return false;
		final File file = new File(path);
		if (!file.exists()) return false;
		if (isBinaryFile(file)) return false;
		return editor.open(file);
	}

	/**
	 * Creates the given file in the registered legacy editor, if any.
	 *
	 * @param title the title of the file to create
	 * @param content the text of the file to be created
	 * @return whether the fule was opened successfully
	 */
	@Override
	public boolean createInEditor(final String title, final String content) {
		if (editor == null) return false;
		return editor.create(title, content);
	}

	@Override
	public void addMenuItem(final String menuPath, final String command) {
		super.addMenuItem(menuPath, command);

		// We want to parse plugins.config classpath resources once per menu
		// initialization, after local plugins from plugins.dir et al. have been
		// populated. A convenient time to do it is when this method is called for
		// the final time, which will be for the File>Quit command.
		if (!"File>Quit".equals(menuPath)) return;

		// Parse plugins.config files from the system classpath, to make them
		// available in more scenarios, such as when no plugins.dir is set.
		// Plugins are detected on non-comment (#) lines, one plugin per line
		// Each plugin definition consists of three parts separated by commas:
		// Menu path without spaces, Menu Entry in quotes, Java class with optional
		// parameters
		//
		// For example, both of these are valid plugin lines:
		// File>Import, "My Plugin (Win)", my.Plugin("location=[Local machine] ")
		// Image>Video Editing, "Move Roi", video2.Move_Roi
		//
		// Note that we currently ignore line separator lines such as:
		// Plugins>Image5D, "-"
		final Pattern pattern = Pattern.compile(
			"^\\s*([^,]*),\\s*\"([^\"]*)\",\\s*([^\\s]*(\\(.*\\))?)\\s*");
		final ClassLoader cl = Context.getClassLoader();
		final IJ1Helper helper = helper();
		try {
			final Enumeration<URL> pluginsConfigs = cl.getResources("plugins.config");
			while (pluginsConfigs.hasMoreElements()) {
				final URL pluginsConfig = pluginsConfigs.nextElement();
				try (final BufferedReader r = new BufferedReader( //
					new InputStreamReader(pluginsConfig.openStream())))
				{
					while (true) {
						final String line = r.readLine();
						if (line == null) break;
						if (line.startsWith("#")) continue; // skip comments
						final Matcher m = pattern.matcher(line);
						if (!m.matches()) continue;
						final String mPath = m.group(1);
						final String label = m.group(2);
						final String plugin = m.group(3);
						helper.addCommand(mPath, label, plugin);
					}
				}
			}
		}
		catch (final IOException exc) {
			log.error(exc);
		}
	}

	@Override
	public Object interceptOpen(final String path, final int planeIndex,
		final boolean display)
	{
		for (final LegacyOpener opener : legacyOpeners) {
			final Object result = opener.open(path, planeIndex, display);
			if (result != null) return result;
		}
		return null;
	}

	@Override
	public Object interceptFileOpen(final String path) {
		for (final LegacyOpener opener : legacyOpeners) {
			final Object result = opener.open(path, -1, true);
			if (result != null) return result;
		}
		return super.interceptFileOpen(path);
	}

	@Override
	public Object interceptOpenImage(final String path, final int planeIndex) {
		for (final LegacyOpener opener : legacyOpeners) {
			final Object result = opener.open(path, planeIndex, false);
			if (result != null) return result;
		}
		return super.interceptFileOpen(path);
	}

	@Override
	public Object interceptOpenRecent(final String path) {
		for (final LegacyOpener opener : legacyOpeners) {
			final Object result = opener.open(path, -1, true);
			if (result != null) return result;
		}
		return super.interceptFileOpen(path);
	}

	@Override
	public Object interceptDragAndDropFile(final File f) {
		if (f.getName().endsWith(".lut")) return null;
		String path;
		try {
			path = f.getCanonicalPath();
			for (final LegacyOpener opener : legacyOpeners) {
				final Object result = opener.open(path, -1, true);
				if (result != null) return result;
			}
		}
		catch (final IOException e) {
			log.error(e);
		}
		return super.interceptDragAndDropFile(f);
	}

	@Override
	public boolean interceptKeyPressed(final KeyEvent e) {
		String accelerator = KeyStroke.getKeyStrokeForEvent(e).toString();
		if (accelerator.startsWith("pressed ")) {
			accelerator = accelerator.substring("pressed ".length());
		}
		return legacyService.handleShortcut(accelerator) ||
			(!e.isControlDown() && legacyService.handleShortcut("control " +
				accelerator));
	}

	@Override
	public Iterable<Thread> getThreadAncestors() {
		final ThreadService threadService = threadService();
		if (threadService == null) return null;
		final Thread current = Thread.currentThread();
		final Set<Thread> seen = new HashSet<>();
		seen.add(current);
		return new Iterable<Thread>() {

			@Override
			public Iterator<Thread> iterator() {
				return new Iterator<Thread>() {

					private Thread thread = threadService.getParent(current);

					@Override
					public boolean hasNext() {
						return thread != null;
					}

					@Override
					public Thread next() {
						final Thread next = thread;
						thread = threadService.getParent(thread);
						if (seen.contains(thread)) {
							thread = null;
						}
						else {
							seen.add(thread);
						}
						return next;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

		};
	}

	@Override
	public boolean interceptCloseAllWindows() {
		final Window[] windows = Window.getWindows();
		boolean continueClose = true;
		final List<Window> confirmableWindows = new ArrayList<>();
		final List<Window> unconfirmableWindows = new ArrayList<>();

		// For each Window, we split them into confirmable or unconfirmable based
		// on whether or not they implement CloseConfirmable. As CloseAllWindows
		// is synchronized on the WindowManager and disposal typically calls
		// WindowManager.removeWindow (also synchronized), disposal is expected
		// to block the EDT while we're still executing CloseAllWindows. As the
		// EDT is necessary to display confirmation dialogs we can not request
		// close confirmation while disposal is in progress without deadlocking.
		// Thus we process and temporarily hide CloseConfirmable windows, then
		// queue up a mass disposal when user input is no longer required.
		// NB: this likely creates a race condition of disposal with System.exit
		// being called by ImageJ 1.x's quitting routine. Thus disposal should
		// never be required to execute before shutting down the JVM. When such
		// behavior is required, the appropriate window should just implement
		// CloseConfirmable.
		for (int w = windows.length - 1; w >= 0 && continueClose; w--) {
			final Window win = windows[w];

			// Skip the ImageJ 1.x main window
			if (win == null || win == helper().getIJ()) {
				continue;
			}

			final Class<?> winClass = win.getClass();
			if (CloseConfirmable.class.isAssignableFrom(winClass)) {
				// Any CloseConfirmable window will have its confirmClose method
				// called.
				// If this operation was not canceled, we hide the window until it can
				// be disposed safely later.
				continueClose = ((CloseConfirmable) win).confirmClose();
				if (continueClose) {
					confirmableWindows.add(win);
					win.setVisible(false);
				}
			}
			else {
				final Package winPackage = winClass.getPackage();
				final String name = winPackage == null ? null : winPackage.getName();

				// Allowlist any classes from ij.* to be disposed. This should get
				// around any offenders in core ImageJ that leave windows open when
				// closing.
				// External classes should just implement CloseConfirmable!
				if (name != null && name.startsWith("ij.")) {
					unconfirmableWindows.add(win);
				}
			}
		}

		// We always want to dispose any CloseConfirmable windows that were
		// successfully added to this list (and thus were not canceled) as
		// these windows were expected to be closed.
		disposeWindows(confirmableWindows);

		// If the close process was canceled, we do not queue disposal for the
		// unconfirmable windows.
		if (!continueClose) return false;

		// Dispose remaining windows and return true to indicate the close
		// operation should continue.
		disposeWindows(unconfirmableWindows);

		return true;
	}

	@Override
	public void interceptImageWindowClose(final Object window) {
		final Frame w = (Frame)window;
		final IJ1Helper helper = helper();
		// When quitting, IJ1 doesn't dispose closing ImageWindows.
		// If the quit is later canceled this would leave orphaned windows.
		// Thus we queue any closed windows for disposal.
		if (helper.isWindowClosed(w) && helper.quitting()) {
			threadService().queue(new Runnable() {
				@Override
				public void run() {
					w.dispose();
				}
			});
		}
	}

	@Override
	public boolean disposing() {
		// NB: At this point, ImageJ1 is in the process of shutting down from
		// within its ij.ImageJ#run() method, which is typically, but not always,
		// called on a separate thread by ij.ImageJ#quit(). The question is: did
		// the shutdown originate from an IJ1 code path, or a SciJava one?
		if (helper().isDisposing()) {
			// NB: ImageJ1 is in the process of a hard shutdown via an API call on
			// the SciJava level. It was probably either LegacyService#dispose() or
			// LegacyUI#dispose(), either of which triggers IJ1Helper#dispose().
			// In that case, we need do nothing else.
		}
		else {
			// NB: ImageJ1 is in the process of a soft shutdown via an API call to
			// ij.ImageJ#quit().
			// In this case, we must dispose the SciJava context too.
			legacyService.getContext().dispose();
		}
		return true;
	}

	// -- Helper methods --

	/**
	 * Convenience method for accessing the attached {@link LegacyService}'s
	 * {@link IJ1Helper}.
	 */
	private IJ1Helper helper() {
		// NB: although there is a setter for the IJ1Helper, it is documented as
		// "non-API" and thus the helper should never be null. If this changes at
		// some point we should do null checking here.
		return legacyService.getIJ1Helper();
	}

	/**
	 * Determines whether a file is binary or text.
	 * <p>
	 * This just checks for a NUL in the first 1024 bytes. Not the best test, but
	 * a pragmatic one.
	 * </p>
	 *
	 * @param file the file to test
	 * @return whether it is binary
	 */
	private static boolean isBinaryFile(final File file) {
		final byte[] buffer = new byte[1024];
		int offset = 0;
		try (final InputStream in = new FileInputStream(file)) {
			while (offset < buffer.length) {
				final int count = in.read(buffer, offset, buffer.length - offset);
				if (count < 0) break;
				offset += count;
			}
		}
		catch (final IOException e) {
			return false;
		}
		while (offset > 0) {
			if (buffer[--offset] == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines whether the current stack trace contains the specified string.
	 *
	 * @param needle the text to find
	 * @return whether the stack trace contains the text
	 */
	private static boolean stackTraceContains(final String needle) {
		final StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		// exclude elements up to, and including, the caller
		for (int i = 3; i < trace.length; i++) {
			if (trace[i].toString().contains(needle)) return true;
		}
		return false;
	}

	// TODO: move to scijava-common?
	private <PT extends SciJavaPlugin> PT createInstanceOfType(
		final Class<PT> type)
	{
		final PluginService pluginService = pluginService();
		if (pluginService == null) return null;
		final PluginInfo<PT> info =
			ListUtils.first(pluginService.getPluginsOfType(type));
		return info == null ? null : pluginService.createInstance(info);
	}

	/**
	 * Helper method to {@link Window#dispose()} all {@code Windows} in a given
	 * list.
	 */
	private void disposeWindows(final List<Window> toDispose) {
		// Queue the disposal to avoid deadlocks
		threadService().queue(new Runnable() {

			@Override
			public void run() {
				for (final Window win : toDispose) {
					win.dispose();
				}
			}
		});
	}

	private ThreadService threadService() {
		return legacyService.getContext().getService(ThreadService.class);
	}

	private PluginService pluginService() {
		return legacyService.getContext().getService(PluginService.class);
	}

}
