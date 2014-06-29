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

package net.imagej.legacy;

import ij.Executer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Menus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Toolbar;
import ij.io.Opener;
import ij.macro.Interpreter;
import ij.plugin.Commands;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Window;
import java.awt.image.ImageProducer;
import java.io.File;
import java.io.IOException;
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

import javax.swing.SwingUtilities;

import net.imagej.display.ImageDisplay;
import net.imagej.patcher.LegacyHooks;

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
 * The DefaultLegacyService needs to patch ImageJ 1.x's classes before they are
 * loaded. Unfortunately, this is tricky: if the DefaultLegacyService already
 * uses those classes, it is a matter of luck whether we can get the patches in
 * before those classes are loaded.
 * </p>
 * <p>
 * Therefore, we put as much interaction with ImageJ 1.x as possible into this
 * class and keep a reference to it in the DefaultLegacyService.
 * </p>
 * 
 * @author Johannes Schindelin
 */
public class IJ1Helper extends AbstractContextual {

	/** A reference to the legacy service, just in case we need it. */
	private final DefaultLegacyService legacyService;

	@Parameter
	private LogService log;

	public IJ1Helper(final DefaultLegacyService legacyService) {
		setContext(legacyService.getContext());
		this.legacyService = legacyService;
	}

	public void initialize() {
		// initialize legacy ImageJ application
		final ImageJ ij1 = IJ.getInstance();
		if (Menus.getCommands() == null) {
			IJ.runPlugIn("ij.IJ.init", "");
		}
		if (ij1 != null) {
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
				final LegacyHooks hooks =
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
			// This is necessary because the ImageJ 1.x window will not set its location
			// if created with mode NO_SHOW, which is exactly how it is created right
			// now by the legacy layer. This is a work-around by ensuring the preferred
			// (e.g. saved and loaded) location is current at the time the IJ1Helper
			// is initialized. Ideally we would like to handle positioning via
			// the LegacyUI though, so that we can restore positions on secondary
			// monitors and such.
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
		if (ij == null) return; // no ImageJ1 to dispose
		if (ij.quitting()) return; // ImageJ1 is already on its way out

		closeImageWindows();
		disposeNonImageWindows();

		// quit legacy ImageJ on the same thread
		ij.exitWhenQuitting(false); // do *not* quit the JVM!
		ij.run();
	}

	/** Add name aliases for ImageJ1 classes to the ScriptService. */
	public void addAliases(final ScriptService scriptService) {
		scriptService.addAlias(ImagePlus.class);
	}

	public boolean isVisible() {
		final ImageJ ij = IJ.getInstance();
		if (ij == null) return false;
		return ij.isVisible();
	}

	private boolean batchMode;

	void setBatchMode(boolean batch) {
		Interpreter.batchMode = batch;
		batchMode = batch;
	}

	void invalidateInstance() {
		try {
			final Method cleanup = IJ.class.getDeclaredMethod("cleanup");
			cleanup.setAccessible(true);
			cleanup.invoke(null);
		} catch (Throwable t) {
			t.printStackTrace();
			legacyService.log().error(t);
		}
	}

	public void setVisible(boolean toggle) {
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

	public void setKeyDown(int keyCode) {
		IJ.setKeyDown(keyCode);
	}

	public void setKeyUp(int keyCode) {
		IJ.setKeyUp(keyCode);
	}

	public boolean hasInstance() {
		return IJ.getInstance() != null;
	}

	public String getVersion() {
		return ImageJ.VERSION;
	}

	public boolean isMacintosh() {
		return IJ.isMacintosh();
	}

	public void setStatus(String message) {
		IJ.showStatus(message);
	}

	public void setProgress(int val, int max) {
		IJ.showProgress(val, max);
	}

	public Component getToolBar() {
		return Toolbar.getInstance();
	}

	public ImageJ getIJ() {
		if (hasInstance()) {
			return IJ.getInstance();
		}
		return null;
	}

	public void showMessage(String title, String message) {
		IJ.showMessage(title, message);
	}

	public boolean showMessageWithCancel(String title, String message) {
		return IJ.showMessageWithCancel(title, message);
	}

	public String commandsName() {
		return Commands.class.getName();
	}

	public void updateRecentMenu(final String path) {
		Menu menu = Menus.getOpenRecentMenu();
		if (menu == null) return;
		int n = menu.getItemCount();
		int index = -1;
		for (int i=0; i<n; i++) {
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
			int count = menu.getItemCount();
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
	 * Gets a macro parameter of type <i>boolean</i>.
	 * 
	 * @param label
	 *            the name of the macro parameter
	 * @param defaultValue
	 *            the default value
	 * @return the boolean value
	 */
	public static boolean getMacroParameter(String label, boolean defaultValue) {
		return getMacroParameter(label) != null || defaultValue;
	}

	/**
	 * Gets a macro parameter of type <i>double</i>.
	 * 
	 * @param label
	 *            the name of the macro parameter
	 * @param defaultValue
	 *            the default value
	 * @return the double value
	 */
	public static double getMacroParameter(String label, double defaultValue) {
		String value = Macro.getValue(Macro.getOptions(), label, null);
		return value != null ? Double.parseDouble(value) : defaultValue;
	}

	/**
	 * Gets a macro parameter of type {@link String}.
	 * 
	 * @param label
	 *            the name of the macro parameter
	 * @param defaultValue
	 *            the default value
	 * @return the value
	 */
	public static String getMacroParameter(String label, String defaultValue) {
		return Macro.getValue(Macro.getOptions(), label, defaultValue);
	}

	/**
	 * Gets a macro parameter of type {@link String}.
	 * 
	 * @param label
	 *            the name of the macro parameter
	 * @return the value, <code>null</code> if the parameter was not specified
	 */
	public static String getMacroParameter(String label) {
		return Macro.getValue(Macro.getOptions(), label, null);
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
			throw new IllegalStateException("Unexpected type of context: " +
				o.getClass().getName());
		}
		return (Context) o;
	}

	/**
	 * Replacement for ImageJ 1.x' MacAdapter.
	 * <p>
	 * ImageJ 1.x has a MacAdapter plugin that intercepts MacOSX-specific events
	 * and handles them. The way it does it is deprecated now, however, and
	 * unfortunately incompatible with the way ImageJ 2's platform service does
	 * it.
	 * </p>
	 * <p>
	 * This class implements the same functionality as the MacAdapter, but in a
	 * way that is compatible with ImageJ 2's platform service.
	 * </p>
	 * @author Johannes Schindelin
	 */
	private static class LegacyEventDelegator extends AbstractContextual {

		@Parameter(required = false)
		private LegacyService legacyService;

		// -- MacAdapter re-implementations --

		/** @param event */
		@EventHandler
		private void onEvent(final AppAboutEvent event)
		{
			if (isLegacyMode()) {
				IJ.run("About ImageJ...");
			}
		}

		/** @param event */
		@EventHandler
		private void onEvent(final AppOpenFilesEvent event) {
			if (isLegacyMode()) {
				final List<File> files = new ArrayList<File>(event.getFiles());
				for (final File file : files) {
					new Opener().openAndAddToRecent(file.getAbsolutePath());
				}
			}
		}

		/** @param event */
		@EventHandler
		private void onEvent(final AppQuitEvent event) {
			if (isLegacyMode()) {
				new Executer("Quit", null); // works with the CommandListener
			}
		}

		/** @param event */
		@EventHandler
		private void onEvent(final AppPreferencesEvent event)
		{
			if (isLegacyMode()) {
				IJ.error("The ImageJ preferences are in the Edit>Options menu.");
			}
		}

		private boolean isLegacyMode() {
			// We call setContext() indirectly from DefaultLegacyService#initialize,
			// therefore legacyService might still be null at this point even if the
			// context knows a legacy service now.
			if (legacyService == null) {
				final Context context = getContext();
				if (context != null) legacyService = context.getService(LegacyService.class);
			}
			return legacyService != null && legacyService.isLegacyMode();
		}

	}

	private static LegacyEventDelegator eventDelegator;

	public static void subscribeEvents(final Context context) {
		if (context == null) {
			eventDelegator = null;
		} else {
			eventDelegator = new LegacyEventDelegator();
			eventDelegator.setContext(context);
			IJ.showStatus("");
		}
	}

	static void run(Class<?> c) {
		IJ.resetEscape();
		if (PlugIn.class.isAssignableFrom(c)) {
			try {
				final PlugIn plugin = (PlugIn) c.newInstance();
				plugin.run("");
			} catch (Exception e) {
				throw e instanceof RuntimeException ? (RuntimeException) e
						: new RuntimeException(e);
			}
			return;
		}
		if (PlugInFilter.class.isAssignableFrom(c)) {
			try {
				final PlugInFilter plugin = (PlugInFilter) c.newInstance();
				ImagePlus image = WindowManager.getCurrentImage();
				if (image != null && image.isLocked()) {
					if (!IJ.showMessageWithCancel("Unlock image?", "The image '" + image.getTitle()
							+ "'appears to be locked... Unlock?"))
						return;
					image.unlock();
				}
				new PlugInFilterRunner(plugin, c.getName(), "");
			} catch (Exception e) {
				throw e instanceof RuntimeException ? (RuntimeException) e
						: new RuntimeException(e);
			}
			return;
		}
		throw new RuntimeException("TODO: construct class loader");
	}

	private boolean menuInitialized;

	/**
	 * Adds legacy-compatible scripts and commands to the ImageJ1 menu structure.
	 */
	public synchronized void addMenuItems() {
		if (menuInitialized) return;
		final Map<String, ModuleInfo> modules =
			legacyService.getScriptsAndNonLegacyCommands();
		@SuppressWarnings("unchecked")
		final Hashtable<String, String> ij1Commands = Menus.getCommands();
		final ImageJ ij1 = getIJ();
		final IJ1MenuWrapper wrapper = ij1 == null ? null : new IJ1MenuWrapper(ij1);
		class Item implements Comparable<Item> {
			private double weight;
			private MenuPath path;
			private String name, identifier;
			private ModuleInfo info;

			@Override
			public int compareTo(Item o) {
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
		final List<Item> items = new ArrayList<Item>();
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
				legacyService.log().info("Overriding " + item.name
					+ "; identifier: " + item.identifier
					+ "; jar: " + ClassUtils.getLocation(item.info.getDelegateClassName()));
				if (wrapper != null) try {
					wrapper.create(item.path, true);
				}
				catch (final Throwable t) {
					legacyService.log().error(t);
				}
			}
			else if (wrapper != null) try {
				wrapper.create(item.path, false);
			}
			catch (final Throwable t) {
				legacyService.log().error(t);
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
		final Map<String, Menu> structure = new HashMap<String, Menu>();
		final Set<Menu> separators = new HashSet<Menu>();

		private IJ1MenuWrapper(final ImageJ ij1) {
			this.ij1 = ij1;
		}

		/**
		 * Creates a {@link MenuItem} matching the structure of the provided path.
		 * Expected path structure is:
		 * <p>
		 * <ul>Level1 > Level2 > ... > Leaf entry</ul>
		 * </p>
		 * <p>
		 * For example, a valid path would be:
		 * </p>
		 * <p>
		 * <ul>Edit > Options > ImageJ2 plugins > Discombobulator</ul>
		 * </p>
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
		private int getIndex(Menu menu, String label) {
					// Place export sub-menu after import sub-menu
			if (menu.getLabel().equals("File") && label.equals("Export")) {
				for (int i=0; i<menu.getItemCount(); i++) {
					final MenuItem menuItem = menu.getItem(i);
					if (menuItem.getLabel().equals("Import")) return i + 1;
				}
			}

			//TODO pass and use actual command weight from IJ2.. maybe?
			// No special case: append to end of menu
			return menu.getItemCount();
		}

		/**
		 * Recursive helper method to builds the final {@link Menu} structure.
		 */
		private Menu getParentMenu(final MenuPath menuPath, int depth) {
			final MenuEntry currentItem = menuPath.get(depth);
			final String currentLabel = currentItem.getName();
			// Check to see if we already know the menu associated with the desired
			// label/path
			final Menu cached = structure.get(currentLabel);
			if (cached != null) return cached;

			// We are at the root of the menu, so see if we have a matching menu
			if (depth == 0) {
				// Special case check the help menu
				if ("Help".equals(currentLabel)) {
					final Menu menu = menuBar.getHelpMenu();
					structure.put(currentLabel, menu);
					return menu;
				}
				// Check the other menus of the menu bar to see if our desired label
				// already exists
				for (int i = 0; i < menuBar.getMenuCount(); i++) {
					final Menu menu = menuBar.getMenu(i);
					if (currentLabel.equals(menu.getLabel())) {
						structure.put(currentLabel, menu);
						return menu;
					}
				}
				// Didn't find a match so we have to create a new menu entry
				final Menu menu = new Menu(currentLabel);
				menuBar.add(menu);
				structure.put(currentLabel, menu);
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
						structure.put(currentLabel, menu);
						return menu;
					}
					// Found a match but it was an existing non-menu item, so our menu
					// structure is invalid.
					//TODO consider mangling the IJ2 menu name instead...
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

			structure.put(currentLabel, menu);
			return menu;
		}

	}

	/**
	 * Evaluates the specified macro.
	 * 
	 * @param macro the macro to evaluate
	 * @return the return value
	 */
	public String runMacro(final String macro) {
		final Thread thread = Thread.currentThread();
		final String name = thread.getName();
		try {
			// to make getOptions() work
			if (!name.startsWith("Run$_")) thread.setName("Run$_" + name);
			return IJ.runMacro(macro);
		}
		finally {
			thread.setName(name);
		}
	}

	/**
	 * Evaluates the specified macro.
	 * 
	 * @param path the macro file to evaluate
	 * @param arg the macro argument
	 * @return the return value
	 */
	public String runMacroFile(final String path, final String arg) {
		final Thread thread = Thread.currentThread();
		final String name = thread.getName();
		try {
			// to make getOptions() work
			if (!name.startsWith("Run$_")) thread.setName("Run$_" + name);
			return IJ.runMacroFile(path, arg);
		}
		finally {
			thread.setName(name);
		}
	}

	/**
	 * Opens an image using ImageJ 1.x.
	 * 
	 * @param path the image file to open
	 * @return the image
	 */
	public Object openImage(final String path) {
		return IJ.openImage(path);
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
	public void handleException(Throwable e) {
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
	public Object getOptions() {
		return Macro.getOptions();
	}

	/**
	 * Delegates to ImageJ 1.x' {@link Macro#setOptions(String)} function.
	 * 
	 * @param options the macro options, or null to reset
	 */
	public void setOptions(final String options) {
		Macro.setOptions(options);
	}

	// -- Helper methods --

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
		for (Frame frame : WindowManager.getNonImageWindows()) {
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
		for (String title : WindowManager.getNonImageTitles()) {
			final Window window = WindowManager.getWindow(title);
			// NB: We can NOT set these windows as active and run the Commands
			// plugin with argument "close", because the default behavior is to
			// try closing the window as an Image. As we know these are not Images,
			// that is never the right thing to do.
			WindowManager.removeWindow(window);
			window.dispose();
		}
	}

}
