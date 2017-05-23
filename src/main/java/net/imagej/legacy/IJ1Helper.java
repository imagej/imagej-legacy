/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
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

package net.imagej.legacy;

import ij.Executer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Menus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Toolbar;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.io.SaveDialog;
import ij.macro.Interpreter;
import ij.measure.ResultsTable;
import ij.plugin.Commands;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Window;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

import net.imagej.display.ImageDisplay;
import net.imagej.legacy.ui.SearchBar;
import net.imagej.patcher.LegacyHooks;
import net.miginfocom.swing.MigLayout;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.MenuEntry;
import org.scijava.MenuPath;
import org.scijava.event.EventHandler;
import org.scijava.log.LogService;
import org.scijava.module.ModuleInfo;
import org.scijava.platform.event.AppAboutEvent;
import org.scijava.platform.event.AppOpenFilesEvent;
import org.scijava.platform.event.AppPreferencesEvent;
import org.scijava.platform.event.AppQuitEvent;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptService;
import org.scijava.util.ClassUtils;

/**
 * A helper class to interact with ImageJ 1.x.
 * <p>
 * The LegacyService needs to patch ImageJ 1.x's classes before they are loaded.
 * Unfortunately, this is tricky: if the LegacyService already uses those
 * classes, it is a matter of luck whether we can get the patches in before
 * those classes are loaded.
 * </p>
 * <p>
 * Therefore, we put as much interaction with ImageJ 1.x as possible into this
 * class and keep a reference to it in the LegacyService.
 * </p>
 *
 * @author Johannes Schindelin
 */
public class IJ1Helper extends AbstractContextual {

	/** A reference to the legacy service, just in case we need it. */
	private final LegacyService legacyService;

	@Parameter
	private LogService log;

	/** Search bar in the main window. */
	private SearchBar searchBar;

	/** Whether we are in the process of forcibly shutting down ImageJ1. */
	private boolean disposing;

	public IJ1Helper(final LegacyService legacyService) {
		setContext(legacyService.getContext());
		this.legacyService = legacyService;
	}

	public void initialize() {
		// initialize legacy ImageJ application
		final ImageJ ij1 = IJ.getInstance();
		addSearchBar(ij1);
		if (getCommands() == null) {
			IJ.runPlugIn("ij.IJ.init", "");
		}
		if (ij1 != null) {
			// NB: *Always* call System.exit(0) when quitting:
			//
			// - In the case of batch mode, the JVM needs to terminate at the
			// conclusion of the macro/script, regardless of the actions performed
			// by that macro/script.
			//
			// - In the case of GUI mode, the JVM needs to terminate when the user
			// quits the program because ImageJ1 has many plugins which do not
			// properly clean up their resources. This is a vicious cycle:
			// ImageJ1's main method sets exitWhenQuitting to true, which has
			// historically masked the problems with these plugins. So we have
			// little choice but to continue this tradition, at least with the
			// legacy ImageJ1 user interface.
			ij1.exitWhenQuitting(true);

			// make sure that the Event Dispatch Thread's class loader is set
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					Thread.currentThread().setContextClassLoader(IJ.getClassLoader());
				}
			});

			final LegacyImageMap imageMap = legacyService.getImageMap();
			for (int i = 1; i <= WindowManager.getImageCount(); i++) {
				imageMap.registerLegacyImage(WindowManager.getImage(i));
			}

			// set icon and title of main window (which are instantiated before the
			// initializer is called)
			try {
				final LegacyHooks hooks = //
					(LegacyHooks) IJ.class.getField("_hooks").get(null);
				ij1.setTitle(hooks.getAppName());
				final URL iconURL = hooks.getIconURL();
				if (iconURL != null) try {
					final Object producer = iconURL.getContent();
					final Image image = ij1.createImage((ImageProducer) producer);
					ij1.setIconImage(image);
					if (IJ.isMacOSX()) try {
						// NB: We also need to set the dock icon
						final Class<?> clazz = Class.forName("com.apple.eawt.Application");
						final Object app = clazz.getMethod("getApplication").invoke(null);
						clazz.getMethod("setDockIconImage", Image.class).invoke(app, image);
					}
					catch (final Throwable t) {
						t.printStackTrace();
					}
				}
				catch (final IOException e) {
					IJ.handleException(e);
				}
			}
			catch (final Throwable t) {
				t.printStackTrace();
			}

			// FIXME: handle window location via LegacyUI
			// This is necessary because the ImageJ 1.x window will not set its
			// location if created with mode NO_SHOW, which is exactly how it is
			// created right now by the legacy layer. This is a work-around by
			// ensuring the preferred (e.g. saved and loaded) location is current at
			// the time the IJ1Helper is initialized. Ideally we would like to handle
			// positioning via the LegacyUI though, so that we can restore positions
			// on secondary monitors and such.
			ij1.setLocation(ij1.getPreferredLocation());
		}
	}

	/**
	 * Forcibly shuts down ImageJ1, with no user interaction or opportunity to
	 * cancel. If ImageJ1 is not currently initialized, or if ImageJ1 is already
	 * in the process of quitting (i.e., {@link ij.ImageJ#quitting()} returns
	 * {@code true}), then this method does nothing.
	 */
	public synchronized void dispose() {
		final ImageJ ij = IJ.getInstance();
		if (ij != null && ij.quitting()) return; // IJ1 is already on its way out

		disposing = true;

		closeImageWindows();
		disposeNonImageWindows();

		if (ij != null) {
			// quit legacy ImageJ on the same thread
			ij.exitWhenQuitting(false); // do *not* quit the JVM!
			ij.run();
		}
		disposing = false;
	}

	/** Whether we are in the process of forcibly shutting down ImageJ1. */
	public boolean isDisposing() {
		return disposing;
	}

	/** Add name aliases for ImageJ1 classes to the ScriptService. */
	public void addAliases(final ScriptService scriptService) {
		scriptService.addAlias(ImagePlus.class);
		scriptService.addAlias(ResultsTable.class);
		scriptService.addAlias(RoiManager.class);
	}

	public boolean isVisible() {
		final ImageJ ij = IJ.getInstance();
		if (ij == null) return false;
		return ij.isVisible();
	}

	/**
	 * Determines whether <i>Edit&gt;Options&gt;Misc...&gt;Run single instance
	 * listener</i> is set.
	 *
	 * @return true if <i>Run single instance listener</i> is set
	 */
	public boolean isRMIEnabled() {
		return Prefs.runSocketListener;
	}

	private boolean batchMode;

	void setBatchMode(final boolean batch) {
		Interpreter.batchMode = batch;
		batchMode = batch;
	}

	void invalidateInstance() {
		try {
			final Method cleanup = IJ.class.getDeclaredMethod("cleanup");
			cleanup.setAccessible(true);
			cleanup.invoke(null);
		}
		catch (final Throwable t) {
			t.printStackTrace();
			log.error(t);
		}
	}

	/**
	 * Sets {@link WindowManager} {@code checkForDuplicateName} field.
	 */
	public void setCheckNameDuplicates(final boolean checkDuplicates) {
		WindowManager.checkForDuplicateName = checkDuplicates;
	}

	public void setVisible(final boolean toggle) {
		if (batchMode) return;
		final ImageJ ij = IJ.getInstance();
		if (ij != null) {
			if (toggle) ij.pack();
			ij.setVisible(toggle);
		}

		// hide/show the legacy ImagePlus instances
		final LegacyImageMap imageMap = legacyService.getImageMap();
		for (final ImagePlus imp : imageMap.getImagePlusInstances()) {
			final ImageWindow window = imp.getWindow();
			if (window != null) window.setVisible(toggle);
		}
	}

	public void syncActiveImage(final ImageDisplay activeDisplay) {
		final LegacyImageMap imageMap = legacyService.getImageMap();
		final ImagePlus activeImagePlus = imageMap.lookupImagePlus(activeDisplay);
		// NB - old way - caused probs with 3d Project
		// WindowManager.setTempCurrentImage(activeImagePlus);
		// NB - new way - test thoroughly
		if (activeImagePlus == null) WindowManager.setCurrentWindow(null);
		else WindowManager.setCurrentWindow(activeImagePlus.getWindow());
	}

	public void setKeyDown(final int keyCode) {
		IJ.setKeyDown(keyCode);
	}

	public void setKeyUp(final int keyCode) {
		IJ.setKeyUp(keyCode);
	}

	public boolean hasInstance() {
		return IJ.getInstance() != null;
	}

	/** Gets the version of ImageJ 1.x. */
	public String getVersion() {
		// NB: We cannot hardcode a reference to ImageJ.VERSION. Java often inlines
		// string constants at compile time. This means that if a different version
		// of ImageJ 1.x is used at runtime, this method could return an incorrect
		// value. Calling IJ.getVersion() would be more reliable, except that we
		// override its behavior to return LegacyHooks#getAppVersion(). So instead,
		// we resort to referencing the ImageJ#VERSION constant via reflection.
		try {
			final Field field = ImageJ.class.getField("VERSION");
			if (field != null) {
				final Object version = field.get(null);
				if (version != null) return version.toString();
			}
		}
		catch (final NoSuchFieldException exc) {
			log.error(exc);
		}
		catch (final IllegalAccessException exc) {
			log.error(exc);
		}
		return "Unknown";
	}

	public boolean isMacintosh() {
		return IJ.isMacintosh();
	}

	public void setStatus(final String message) {
		IJ.showStatus(message);
	}

	public void setProgress(final int val, final int max) {
		IJ.showProgress(val, max);
	}

	public Component getToolBar() {
		return Toolbar.getInstance();
	}

	public Panel getStatusBar() {
		if (!hasInstance()) return null;
		return IJ.getInstance().getStatusBar();
	}

	public SearchBar getSearchBar() {
		return searchBar;
	}

	public Frame getIJ() {
		if (hasInstance()) {
			return IJ.getInstance();
		}
		return null;
	}

	public void setLocation(final int x, final int y) {
		if (!hasInstance()) return;
		IJ.getInstance().setLocation(x, y);
	}

	public int getX() {
		if (!hasInstance()) return 0;
		return IJ.getInstance().getX();
	}

	public int getY() {
		if (!hasInstance()) return 0;
		return IJ.getInstance().getY();
	}

	public boolean isWindowClosed(final Frame window) {
		if (window instanceof ImageWindow) {
			return ((ImageWindow) window).isClosed();
		}
		return false;
	}

	public boolean quitting() {
		if (hasInstance()) return IJ.getInstance().quitting();
		return false;
	}

	public int[] getIDList() {
		return WindowManager.getIDList();
	}

	public ImagePlus getImage(final int imageID) {
		return WindowManager.getImage(imageID);
	}

	/**
	 * Returns {@link ImagePlus#getTitle()} if the object is an {@link ImagePlus},
	 * otherwise null.
	 */
	public static String getTitle(final Object o) {
		return o instanceof ImagePlus ? ((ImagePlus) o).getTitle() : null;
	}

	public ClassLoader getClassLoader() {
		return IJ.getClassLoader();
	}

	public void showMessage(final String title, final String message) {
		IJ.showMessage(title, message);
	}

	public boolean showMessageWithCancel(final String title,
		final String message)
	{
		return IJ.showMessageWithCancel(title, message);
	}

	public String commandsName() {
		return Commands.class.getName();
	}

	public void updateRecentMenu(final String path) {
		final Menu menu = Menus.getOpenRecentMenu();
		if (menu == null) return;
		final int n = menu.getItemCount();
		int index = -1;
		for (int i = 0; i < n; i++) {
			if (menu.getItem(i).getLabel().equals(path)) {
				index = i;
				break;
			}
		}
		// Move to most recent
		if (index > 0) {
			final MenuItem item = menu.getItem(index);
			menu.remove(index);
			menu.insert(item, 0);
		}
		// not found, so replace oldest
		else if (index < 0) {
			final int count = menu.getItemCount();
			if (count >= Menus.MAX_OPEN_RECENT_ITEMS) {
				menu.remove(count - 1);
			}
			final MenuItem item = new MenuItem(path);
			final ImageJ instance = IJ.getInstance();
			if (instance != null) item.addActionListener(instance);
			menu.insert(item, 0);
		}
		// if index was 0, already at the head so do nothing
	}

	/**
	 * Opens an image and adds the path to the <i>File&gt;Open Recent</i> menu.
	 *
	 * @param file the image to open
	 */
	public static void openAndAddToRecent(final File file) {
		new Opener().openAndAddToRecent(file.getAbsolutePath());
	}

	/**
	 * Records an option in ImageJ 1.x's macro recorder, <em>safely</em>.
	 * <p>
	 * Both the key and the value will be escaped to avoid problems. This behavior
	 * differs from direct calls to {@link Recorder#recordOption(String, String)},
	 * which do not escape either string.
	 * </p>
	 *
	 * @param key the name of the option
	 * @param value the value of the option
	 */
	public void recordOption(final String key, final String value) {
		Recorder.recordOption(escape(key), escape(value));
	}

	/**
	 * Determines whether we're running inside a macro right now.
	 *
	 * @return whether we're running a macro right now.
	 */
	public boolean isMacro() {
		return IJ.isMacro();
	}

	/**
	 * Gets a macro parameter of type <i>boolean</i>.
	 *
	 * @param label the name of the macro parameter
	 * @param defaultValue the default value
	 * @return the boolean value
	 */
	public boolean getMacroParameter(final String label,
		final boolean defaultValue)
	{
		return getMacroParameter(label) != null || defaultValue;
	}

	/**
	 * Gets a macro parameter of type <i>double</i>.
	 *
	 * @param label the name of the macro parameter
	 * @param defaultValue the default value
	 * @return the double value
	 */
	public double getMacroParameter(final String label,
		final double defaultValue)
	{
		final String value = Macro.getValue(getOptions(), label, null);
		return value != null ? Double.parseDouble(value) : defaultValue;
	}

	/**
	 * Gets a macro parameter of type {@link String}.
	 *
	 * @param label the name of the macro parameter
	 * @param defaultValue the default value
	 * @return the value
	 */
	public String getMacroParameter(final String label,
		final String defaultValue)
	{
		return Macro.getValue(getOptions(), label, defaultValue);
	}

	/**
	 * Gets a macro parameter of type {@link String}.
	 *
	 * @param label the name of the macro parameter
	 * @return the value, <code>null</code> if the parameter was not specified
	 */
	public String getMacroParameter(final String label) {
		return Macro.getValue(getOptions(), label, null);
	}

	/** Returns the active macro {@link Interpreter}. */
	public static Object getInterpreter() {
		return Interpreter.getInstance();
	}

	/**
	 * Gets the value of the specified variable, from the given macro
	 * {@link Interpreter}.
	 * 
	 * @param interpreter The macro {@link Interpreter} to query.
	 * @return The list of variables in {@code key\tvalue} form, as given by
	 *         {@link Interpreter#getVariables()}.
	 * @throws ClassCastException if the given interpreter is not an
	 *           {@link Interpreter}.
	 */
	public String[] getVariables(final Object interpreter) {
		return ((Interpreter) interpreter).getVariables();
	}

	/**
	 * Gets the value of the specified variable, from the given macro
	 * {@link Interpreter}.
	 * 
	 * @param interpreter The macro {@link Interpreter} to query.
	 * @param name The name of the variable to retrieve.
	 * @return The value of the requested variable, as either a {@link String}, a
	 *         {@link Double} or {@code null}.
	 * @throws ClassCastException if the given interpreter is not an
	 *           {@link Interpreter}.
	 */
	public Object getVariable(final Object interpreter, final String name) {
		final Interpreter interp = (Interpreter) interpreter;

		// might be a string
		final String sValue = interp.getStringVariable(name);
		if (sValue != null) return sValue;

		// probably a number
		final double nValue = interp.getVariable2(name);
		if (!Double.isNaN(nValue)) return nValue;

		return null;
	}

	/** Returns true if the object is an instance of {@link ImagePlus}. */
	public boolean isImagePlus(final Object o) {
		return o instanceof ImagePlus;
	}

	/** Returns true if the class is assignable to {@link ImagePlus}. */
	public boolean isImagePlus(final Class<?> c) {
		return ImagePlus.class.isAssignableFrom(c);
	}

	/**
	 * Gets the ID of the given {@link ImagePlus} object. If the given object is
	 * not an {@link ImagePlus}, throws {@link IllegalArgumentException}.
	 *
	 * @param o The {@link ImagePlus} whose ID is needed.
	 * @return The value of {@link ImagePlus#getID()}.
	 * @see #isImagePlus(Object)
	 * @throws ClassCastException if the given object is not an {@link ImagePlus}.
	 */
	public int getImageID(final Object o) {
		return ((ImagePlus) o).getID();
	}

	/** Gets the SciJava application context linked to the ImageJ 1.x instance. */
	public static Context getLegacyContext() {
		// NB: This call instantiates a Context if there is none.
		//
		// IJ.runPlugIn() will be intercepted by the legacy hooks if they are
		// installed and return the current Context.
		//
		// If no legacy hooks are installed, ImageJ 1.x will instantiate the Context
		// using the PluginClassLoader and the LegacyService will install the legacy
		// hooks.
		final Object o = IJ.runPlugIn("org.scijava.Context", "");
		if (o == null) return null;
		if (!(o instanceof Context)) {
			throw new IllegalStateException("Unexpected type of context: " + //
				o.getClass().getName());
		}
		return (Context) o;
	}

	/**
	 * Partial replacement for ImageJ 1.x's MacAdapter.
	 * <p>
	 * ImageJ 1.x has a MacAdapter plugin that intercepts MacOSX-specific events
	 * and handles them. The way it does it is deprecated now, however, and
	 * unfortunately incompatible with the way ImageJ 2's platform service does
	 * it.
	 * </p>
	 * <p>
	 * This class implements the same functionality as the MacAdapter, but in a
	 * way that is compatible with the SciJava platform service.
	 * </p>
	 * <p>
	 * Note that the {@link AppAboutEvent}, {@link AppPreferencesEvent} and
	 * {@link AppQuitEvent} are handled separately, indirectly, by the
	 * {@link LegacyImageJApp}. See also {@link IJ1Helper#appAbout},
	 * {@link IJ1Helper#appPrefs} and {@link IJ1Helper#appQuit}.
	 * </p>
	 *
	 * @author Johannes Schindelin
	 */
	private static class LegacyEventDelegator extends AbstractContextual {

		@Parameter(required = false)
		private LegacyService legacyService;

		// -- MacAdapter re-implementations --

		/** @param event */
		@EventHandler
		private void onEvent(final AppOpenFilesEvent event) {
			if (isLegacyMode()) {
				final List<File> files = new ArrayList<>(event.getFiles());
				for (final File file : files) {
					openAndAddToRecent(file);
				}
			}
		}

		private boolean isLegacyMode() {
			// We call setContext() indirectly from LegacyService#initialize,
			// therefore legacyService might still be null at this point even if the
			// context knows a legacy service now.
			if (legacyService == null) {
				final Context context = getContext();
				if (context != null) {
					legacyService = context.getService(LegacyService.class);
				}
			}
			return legacyService != null && legacyService.isLegacyMode();
		}

	}

	private static LegacyEventDelegator eventDelegator;

	public static void subscribeEvents(final Context context) {
		if (context == null) {
			eventDelegator = null;
		}
		else {
			eventDelegator = new LegacyEventDelegator();
			eventDelegator.setContext(context);
		}
	}

	static void run(final Class<?> c) {
		IJ.resetEscape();
		if (PlugIn.class.isAssignableFrom(c)) {
			try {
				final PlugIn plugin = (PlugIn) c.newInstance();
				plugin.run("");
			}
			catch (final Exception e) {
				throw e instanceof RuntimeException ? //
					(RuntimeException) e : new RuntimeException(e);
			}
			return;
		}
		if (PlugInFilter.class.isAssignableFrom(c)) {
			try {
				final PlugInFilter plugin = (PlugInFilter) c.newInstance();
				final ImagePlus image = WindowManager.getCurrentImage();
				if (image != null && image.isLocked()) {
					final String msg = "The image '" + //
						image.getTitle() + "' appears to be locked... Unlock?";
					if (!IJ.showMessageWithCancel("Unlock image?", msg)) return;
					image.unlock();
				}
				new PlugInFilterRunner(plugin, c.getName(), "");
			}
			catch (final Exception e) {
				throw e instanceof RuntimeException ? (RuntimeException) e
					: new RuntimeException(e);
			}
			return;
		}
		throw new RuntimeException("TODO: construct class loader");
	}

	private boolean menuInitialized;

	@SuppressWarnings("unchecked")
	public Hashtable<String, String> getCommands() {
		return Menus.getCommands();
	}

	public MenuBar getMenuBar() {
		final ImageJ ij1 = hasInstance() ? IJ.getInstance() : null;
		return ij1 == null ? null : ij1.getMenuBar();
	}

	/**
	 * Adds legacy-compatible scripts and commands to the ImageJ1 menu structure.
	 */
	public synchronized void addMenuItems() {
		if (menuInitialized) return;
		final Map<String, ModuleInfo> modules = //
			legacyService.getScriptsAndNonLegacyCommands();
		final Hashtable<String, String> ij1Commands = getCommands();
		final ImageJ ij1 = hasInstance() ? IJ.getInstance() : null;
		final IJ1MenuWrapper wrapper = ij1 == null ? null : new IJ1MenuWrapper(ij1);
		class Item implements Comparable<Item> {

			private double weight;
			private MenuPath path;
			private String name, identifier;
			private ModuleInfo info;

			@Override
			public int compareTo(final Item o) {
				if (weight != o.weight) return Double.compare(weight, o.weight);
				return compare(path, o.path);
			}

			public int compare(final MenuPath a, final MenuPath b) {
				int i = 0;
				while (i < a.size() && i < b.size()) {
					final MenuEntry a2 = a.get(i), b2 = b.get(i);
					int diff = Double.compare(a.get(i).getWeight(), b.get(i).getWeight());
					if (diff != 0) return diff;
					diff = a2.getName().compareTo(b2.getName());
					if (diff != 0) return diff;
					i++;
				}
				return 0;
			}
		}
		final List<Item> items = new ArrayList<>();
		for (final Entry<String, ModuleInfo> entry : modules.entrySet()) {
			final String key = entry.getKey();
			final ModuleInfo info = entry.getValue();
			final MenuEntry leaf = info.getMenuPath().getLeaf();
			if (leaf == null) continue;
			final MenuPath path = info.getMenuPath();
			final String name = leaf.getName();
			final Item item = new Item();
			item.weight = leaf.getWeight();
			item.path = path;
			item.name = name;
			item.identifier = key;
			item.info = info;
			items.add(item);
		}
		// sort by menu weight, then alphabetically
		Collections.sort(items);
		for (final Item item : items) {
			if (ij1Commands.containsKey(item.name)) {
				log.info("Overriding " + item.name + //
					"; identifier: " + item.identifier + //
					"; jar: " + ClassUtils.getLocation(item.info.getDelegateClassName()));
				if (wrapper != null) try {
					wrapper.create(item.path, true);
				}
				catch (final Throwable t) {
					log.error(t);
				}
			}
			else if (wrapper != null) try {
				wrapper.create(item.path, false);
			}
			catch (final Throwable t) {
				log.error(t);
			}
			ij1Commands.put(item.name, item.identifier);
		}
		menuInitialized = true;
	}

	/**
	 * Helper class for wrapping ImageJ2 menu paths to ImageJ1 {@link Menu}
	 * structures, and inserting them into the proper positions of the
	 * {@link MenuBar}.
	 */
	private static class IJ1MenuWrapper {

		final ImageJ ij1;
		final MenuBar menuBar = Menus.getMenuBar();
		final MenuCache menuCache = new MenuCache();
		final Set<Menu> separators = new HashSet<>();

		private IJ1MenuWrapper(final ImageJ ij1) {
			this.ij1 = ij1;
		}

		/**
		 * Creates a {@link MenuItem} matching the structure of the provided path.
		 * Expected path structure is:
		 * <ul>
		 * <li>Level1 > Level2 > ... > Leaf entry</li>
		 * </ul>
		 * <p>
		 * For example, a valid path would be:
		 * </p>
		 * <ul>
		 * <li>Edit > Options > ImageJ2 plugins > Discombobulator</li>
		 * </ul>
		 */
		private MenuItem create(final MenuPath path, final boolean reuseExisting) {
			// Find the menu structure where we can insert our command.
			// NB: size - 1 is the leaf position, so we want to go to size - 2 to
			// find the parent menu location
			final Menu menu = getParentMenu(path, path.size() - 2);
			final String label = path.getLeaf().getName();
			// If we are overriding an item, find the item being overridden
			if (reuseExisting) {
				for (int i = 0; i < menu.getItemCount(); i++) {
					final MenuItem item = menu.getItem(i);
					if (label.equals(item.getLabel())) {
						return item;
					}
				}
			}
			if (!separators.contains(menu)) {
				if (menu.getItemCount() > 0) menu.addSeparator();
				separators.add(menu);
			}
			// Otherwise, we are creating a new item
			final MenuItem item = new MenuItem(label);
			menu.insert(item, getIndex(menu, label));
			item.addActionListener(ij1);
			return item;
		}

		/**
		 * Helper method to look up special cases for menu weighting
		 */
		private int getIndex(final Menu menu, final String label) {
			// Place export sub-menu after import sub-menu
			if (menu.getLabel().equals("File") && label.equals("Export")) {
				for (int i = 0; i < menu.getItemCount(); i++) {
					final MenuItem menuItem = menu.getItem(i);
					if (menuItem.getLabel().equals("Import")) return i + 1;
				}
			}

			// TODO pass and use actual command weight from IJ2.. maybe?
			// No special case: append to end of menu
			return menu.getItemCount();
		}

		/** Recursive helper method to build the final {@link Menu} structure. */
		private Menu getParentMenu(final MenuPath menuPath, final int depth) {
			final MenuEntry currentItem = menuPath.get(depth);
			final String currentLabel = currentItem.getName();
			// Check to see if we already know the menu associated with the desired
			// label/path
			final Menu cached = menuCache.get(menuPath, depth);
			if (cached != null) return cached;

			// We are at the root of the menu, so see if we have a matching menu
			if (depth == 0) {
				// Special case check the help menu
				if ("Help".equals(currentLabel)) {
					final Menu menu = menuBar.getHelpMenu();
					menuCache.put(menuPath, depth, menu);
					return menu;
				}
				// Check the other menus of the menu bar to see if our desired label
				// already exists
				for (int i = 0; i < menuBar.getMenuCount(); i++) {
					final Menu menu = menuBar.getMenu(i);
					if (currentLabel.equals(menu.getLabel())) {
						menuCache.put(menuPath, depth, menu);
						return menu;
					}
				}
				// Didn't find a match so we have to create a new menu entry
				final Menu menu = new Menu(currentLabel);
				menuBar.add(menu);
				menuCache.put(menuPath, depth, menu);
				return menu;
			}
			final Menu parent = getParentMenu(menuPath, depth - 1);
			// Once the parent of this entry is obtained, we need to check if it
			// already contains the current entry.
			for (int i = 0; i < parent.getItemCount(); i++) {
				final MenuItem item = parent.getItem(i);
				if (currentLabel.equals(item.getLabel())) {
					if (item instanceof Menu) {
						// Found a menu entry that matches our desired label, so return
						final Menu menu = (Menu) item;
						menuCache.put(menuPath, depth, menu);
						return menu;
					}
					// Found a match but it was an existing non-menu item, so our menu
					// structure is invalid.
					// TODO consider mangling the IJ2 menu name instead...
					throw new IllegalArgumentException("Not a menu: " + currentLabel);
				}
			}
			if (!separators.contains(parent)) {
				if (parent.getItemCount() > 0) parent.addSeparator();
				separators.add(parent);
			}
			// An existing entry in the parent menu was not found, so we need to
			// create a new entry.
			final Menu menu = new Menu(currentLabel);
			parent.insert(menu, getIndex(parent, menu.getLabel()));

			menuCache.put(menuPath, depth, menu);
			return menu;
		}

	}

	private static class MenuCache {

		private final Map<String, Menu> map = new HashMap<>();

		public void put(final MenuPath menuPath, final int depth, final Menu menu) {
			map.put(key(menuPath, depth), menu);
		}

		public Menu get(final MenuPath menuPath, final int depth) {
			return map.get(key(menuPath, depth));
		}

		private String key(final MenuPath menuPath, final int depth) {
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i <= depth; i++) {
				sb.append(menuPath.get(i).getName());
				sb.append("\n"); // NB: an unambiguous separator
			}
			return sb.toString();
		}
	}

	private <T> T runMacroFriendly(final Callable<T> call) {
		if (EventQueue.isDispatchThread()) {
			throw new IllegalStateException("Cannot run macro from the EDT!");
		}
		final Thread thread = Thread.currentThread();
		final String name = thread.getName();
		try {
			// to make getOptions() work
			if (!name.startsWith("Run$_")) thread.setName("Run$_" + name);
			// to make Macro.abort() work
			if (!name.endsWith("Macro$")) thread.setName(thread.getName() + "Macro$");
			return call.call();
		}
		catch (final RuntimeException e) {
			throw e;
		}
		catch (final Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			thread.setName(name);
			// HACK: Try to null out the ij.macro.Interpreter, just in case.
			// See: http://fiji.sc/bugzilla/show_bug.cgi?id=1266
			try {
				final Method m = Interpreter.class.getDeclaredMethod("setInstance",
					Interpreter.class);
				m.setAccessible(true);
				m.invoke(null, new Object[] { null });
			}
			catch (final NoSuchMethodException | IllegalAccessException
					| InvocationTargetException exc)
			{
				log.error(exc);
			}
		}
	}

	/**
	 * Evaluates the specified command.
	 *
	 * @param command the command to execute
	 */
	public void run(final String command) {
		runMacroFriendly(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				IJ.run(command);
				return null;
			}
		});
	}

	/**
	 * Evaluates the specified macro.
	 *
	 * @param macro the macro to evaluate
	 * @return the return value
	 */
	public String runMacro(final String macro) {
		return runMacroFriendly(new Callable<String>() {

			@Override
			public String call() throws Exception {
				return IJ.runMacro(macro);
			}
		});
	}

	/**
	 * Evaluates the specified macro.
	 *
	 * @param path the macro file to evaluate
	 * @param arg the macro argument
	 * @return the return value
	 */
	public String runMacroFile(final String path, final String arg) {
		return runMacroFriendly(new Callable<String>() {

			@Override
			public String call() throws Exception {
				return IJ.runMacroFile(path, arg);
			}
		});
	}

	/**
	 * Opens an image using ImageJ 1.x.
	 *
	 * @param path the image file to open
	 * @return the image
	 */
	public Object openImage(final String path) {
		return openImage(path, false);
	}

	/**
	 * Opens an image using ImageJ 1.x.
	 *
	 * @param path the image file to open
	 * @return the image
	 */
	public Object openImage(final String path, final boolean show) {
		final ImagePlus imp = IJ.openImage(path);
		if (show && imp != null) imp.show();
		return imp;
	}

	/**
	 * Opens a path using ImageJ 1.x, bypassing the (javassisted) IJ utility
	 * class.
	 *
	 * @param path the image file to open
	 */
	public void openPathDirectly(final String path) {
		new Opener().open(path);
	}

	/**
	 * Enables or disables ImageJ 1.x' debug mode.
	 *
	 * @param debug whether to show debug messages or not
	 */
	public void setDebugMode(final boolean debug) {
		IJ.debugMode = debug;
	}

	/**
	 * Delegate exception handling to ImageJ 1.x.
	 *
	 * @param e the exception to handle
	 */
	public void handleException(final Throwable e) {
		IJ.handleException(e);

	}

	/**
	 * Ask ImageJ 1.x whether it thinks whether the Shift key is held down.
	 *
	 * @return whether the Shift key is considered <i>down</i>
	 */
	public boolean shiftKeyDown() {
		return IJ.shiftKeyDown();
	}

	/**
	 * Delegates to ImageJ 1.x' {@link Macro#getOptions()} function.
	 *
	 * @return the macro options, or null
	 */
	public String getOptions() {
		final String options = Macro.getOptions();
		return options == null ? "" : options;
	}

	/**
	 * Delegates to ImageJ 1.x' {@link Macro#setOptions(String)} function.
	 *
	 * @param options the macro options, or null to reset
	 */
	public void setOptions(final String options) {
		Macro.setOptions(options);
	}

	/** Handles shutdown of ImageJ 1.x. */
	public void appQuit() {
		if (legacyService.isLegacyMode()) {
			new Executer("Quit", null); // works with the CommandListener
		}
	}

	/** Displays the About ImageJ 1.x dialog. */
	public void appAbout() {
		if (legacyService.isLegacyMode()) {
			IJ.run("About ImageJ...");
		}
	}

	/** Sets OpenDialog's default directory */
	public void setDefaultDirectory(final File directory) {
		OpenDialog.setDefaultDirectory(directory.getPath());
	}

	/** Uses ImageJ 1.x' OpenDialog */
	public File openDialog(final String title) {
		return openDialog(title, null);
	}

	/** Uses ImageJ 1.x' OpenDialog */
	public File openDialog(final String title, final File file) {
		final String defaultDir = file == null ? null : file.getParent();
		final String defaultName = file == null ? null : file.getName();
		final OpenDialog openDialog = //
			new OpenDialog(title, defaultDir, defaultName);
		final String directory = openDialog.getDirectory();

		// NB: As a side effect, ImageJ1 normally appends the selected
		// file as a macro parameter when the getFileName() is called!
		// We need to suppress that problematic behavior here; see:
		// https://github.com/scijava/scijava-common/issues/235
		final boolean recording = Recorder.record;
		Recorder.record = false;
		final String fileName = openDialog.getFileName();
		Recorder.record = recording;

		if (directory != null && fileName != null) {
			return new File(directory, fileName);
		}
		return null;
	}

	/** Uses ImageJ 1.x' SaveDialog */
	public File saveDialog(final String title, final File file,
		final String extension)
	{
		// Use ImageJ1's SaveDialog.
		final String defaultName, defaultExtension;
		if (file == null) {
			defaultName = null;
			defaultExtension = extension;
		}
		else {
			final int dotIndex = file.getName().indexOf('.');
			if (dotIndex > 0) {
				// split filename from extension
				defaultName = file.getName().substring(0, dotIndex);
				defaultExtension = extension == null ? //
					file.getName().substring(dotIndex) : extension;
			}
			else {
				// file had no extension
				defaultName = file.getName();
				defaultExtension = extension;
			}
		}
		final SaveDialog saveDialog = //
			new SaveDialog(title, defaultName, defaultExtension);
		final String directory = saveDialog.getDirectory();

		// NB: As a side effect, ImageJ1 normally appends the selected
		// file as a macro parameter when the getFileName() is called!
		// We need to suppress that problematic behavior here; see:
		// https://github.com/scijava/scijava-common/issues/235
		final boolean recording = Recorder.record;
		Recorder.record = false;
		final String fileName = saveDialog.getFileName();
		Recorder.record = recording;

		if (directory != null && fileName != null) {
			return new File(directory, fileName);
		}
		return null;
	}

	/** Chooses a directory using ImageJ 1.x' directory chooser. */
	public String getDirectory(final String title, final File file) {
		if (file != null) {
			final String defaultDir = //
				file.isDirectory() ? file.getPath() : file.getParent();
			if (defaultDir != null) DirectoryChooser.setDefaultDirectory(defaultDir);
		}

		// NB: As a side effect, ImageJ1 normally appends the selected
		// directory as a macro parameter when getDirectory() is called!
		// We need to suppress that problematic behavior here; see:
		// https://github.com/scijava/scijava-common/issues/235
		final boolean recording = Recorder.record;
		Recorder.record = false;
		final String directory = new DirectoryChooser(title).getDirectory();
		Recorder.record = recording;

		return directory;
	}

	/** Handles display of the ImageJ 1.x preferences. */
	public void appPrefs() {
		if (legacyService.isLegacyMode()) {
			IJ.error("The ImageJ preferences are in the Edit>Options menu.");
		}
	}

	// -- Helper methods --

	/** Adds a search bar to the main image frame. */
	private void addSearchBar(final Object imagej) {
		if (!(imagej instanceof Window)) return; // NB: Avoid headless issues.

		final Component[] ijc = ((Container) imagej).getComponents();
		if (ijc.length < 2) return;
		final Component ijc1 = ijc[1];
		if (!(ijc1 instanceof Container)) return;

		// rebuild the main panel (status label + progress bar)
		final Container panel = (Container) ijc1;
		final Component[] pc = panel.getComponents();
		panel.removeAll();
		panel.setLayout(new MigLayout("fillx, insets 0", "[0:0]p![p!]"));
		for (final Component c : pc) {
			panel.add(c);
		}

		// add the search bar
		searchBar = new SearchBar(getContext(), (Window) imagej);
		panel.add(searchBar);
	}

	/** Closes all image windows on the event dispatch thread. */
	private void closeImageWindows() {
		// TODO: Consider using ThreadService#invoke to simplify this logic.
		final Runnable run = new Runnable() {

			@Override
			public void run() {
				// close out all image windows, without dialog prompts
				while (true) {
					final ImagePlus imp = WindowManager.getCurrentImage();
					if (imp == null) break;
					imp.changes = false;
					imp.close();
				}
			}
		};
		if (EventQueue.isDispatchThread()) {
			run.run();
		}
		else {
			try {
				EventQueue.invokeAndWait(run);
			}
			catch (final Exception e) {
				// report & ignore
				log.error(e);
			}
		}
	}

	private void disposeNonImageWindows() {
		disposeNonImageFrames();
		disposeOtherNonImageWindows();
	}

	/**
	 * Disposes all the non-image window frames, as given by
	 * {@link WindowManager#getNonImageWindows()}.
	 */
	private void disposeNonImageFrames() {
		for (final Frame frame : WindowManager.getNonImageWindows()) {
			frame.dispose();
		}
	}

	/**
	 * Ensures <em>all</em> the non-image windows are closed.
	 * <p>
	 * This is a non-trivial problem, as
	 * {@link WindowManager#getNonImageWindows()} <em>only</em> returns
	 * {@link Frame}s. However there are non-image, non-{@link Frame} windows that
	 * are critical to close: for example, the
	 * {@link ij.plugin.frame.ContrastAdjuster} spawns a polling thread to do its
	 * work, which will continue to run until the {@code ContrastAdjuster} is
	 * explicitly closed.
	 * </p>
	 */
	private void disposeOtherNonImageWindows() {
		// NB: As of v1.49b, getNonImageTitles is not restricted to Frames,
		// so we can use it to iterate through the available windows.
		for (final String title : WindowManager.getNonImageTitles()) {
			final Window window = WindowManager.getWindow(title);
			// NB: We can NOT set these windows as active and run the Commands
			// plugin with argument "close", because the default behavior is to
			// try closing the window as an Image. As we know these are not Images,
			// that is never the right thing to do.
			WindowManager.removeWindow(window);
			window.dispose();
		}
	}

	/** Escapes the given string according to the Java language specification. */
	private String escape(final String s) {
		// NB: It would be nice to use the StringEscapeUtils.escapeJava method of
		// Apache Commons Lang, but we eschew it for now to avoid the dependency.

		// escape quotes and backslashes
		String escaped = s.replaceAll("([\"\\\\])", "\\\\$1");

		// escape special characters
		escaped = escaped.replaceAll("\b", "\\\\b");
		escaped = escaped.replaceAll("\n", "\\\\n");
		escaped = escaped.replaceAll("\t", "\\\\t");
		escaped = escaped.replaceAll("\f", "\\\\f");
		escaped = escaped.replaceAll("\r", "\\\\r");

		return escaped;
	}

}
