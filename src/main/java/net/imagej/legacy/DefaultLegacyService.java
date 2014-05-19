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

import ij.Menus;

import java.awt.GraphicsEnvironment;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import net.imagej.DatasetService;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;
import net.imagej.legacy.plugin.LegacyCommand;
import net.imagej.legacy.plugin.LegacyCompatibleCommand;
import net.imagej.legacy.plugin.LegacyPluginFinder;
import net.imagej.options.OptionsChannels;
import net.imagej.patcher.LegacyInjector;
import net.imagej.threshold.ThresholdService;
import net.imagej.ui.viewer.image.ImageDisplayViewer;

import org.scijava.app.StatusService;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.display.event.DisplayActivatedEvent;
import org.scijava.display.event.input.KyPressedEvent;
import org.scijava.display.event.input.KyReleasedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.input.KeyCode;
import org.scijava.log.LogService;
import org.scijava.menu.MenuService;
import org.scijava.menu.ShadowMenu;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.options.OptionsService;
import org.scijava.options.event.OptionsEvent;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.ui.ApplicationFrame;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.util.ColorRGB;

/**
 * Default service for working with legacy ImageJ 1.x.
 * <p>
 * The legacy service overrides the behavior of various legacy ImageJ methods,
 * inserting seams so that (e.g.) the modern UI is aware of legacy ImageJ events
 * as they occur.
 * </p>
 * <p>
 * It also maintains an image map between legacy ImageJ {@link ij.ImagePlus}
 * objects and modern ImageJ {@link ImageDisplay}s.
 * </p>
 * <p>
 * In this fashion, when a legacy command is executed on a {@link ImageDisplay},
 * the service transparently translates it into an {@link ij.ImagePlus}, and vice
 * versa, enabling backward compatibility with legacy commands.
 * </p>
 * 
 * @author Barry DeZonia
 * @author Curtis Rueden
 * @author Johannes Schindelin
 * @author Mark Hiner
 */
@Plugin(type = Service.class)
public final class DefaultLegacyService extends AbstractService implements
	LegacyService, ActionListener
{
	static {
		LegacyInjector.preinit();
	}

	@Parameter
	private OverlayService overlayService;

	@Parameter
	private LogService log;

	@Parameter
	private EventService eventService;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private CommandService commandService;

	@Parameter
	private OptionsService optionsService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private ThresholdService thresholdService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private MenuService menuService;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private StatusService statusService;

	@Parameter(required = false)
	private UIService uiService;

	private static DefaultLegacyService instance;

	/** Mapping between modern and legacy image data structures. */
	private LegacyImageMap imageMap;

	/** Method of synchronizing modern & legacy options. */
	private OptionsSynchronizer optionsSynchronizer;

	/** Keep references to ImageJ 1.x separate */
	private IJ1Helper ij1Helper;

	public IJ1Helper getIJ1Helper() {
		return ij1Helper;
	}

	private ThreadLocal<Boolean> isProcessingEvents = new ThreadLocal<Boolean>();

	// -- LegacyService methods --

	@Override
	public LogService log() {
		return log;
	}

	@Override
	public StatusService status() {
		return statusService;
	}

	@Override
	public LegacyImageMap getImageMap() {
		return imageMap;
	}

	@Override
	public OptionsSynchronizer getOptionsSynchronizer() {
		return optionsSynchronizer;
	}

	@Override
	public void
		runLegacyCommand(final String ij1ClassName, final String argument)
	{
		final String arg = argument == null ? "" : argument;
		final Map<String, Object> inputMap = new HashMap<String, Object>();
		inputMap.put("className", ij1ClassName);
		inputMap.put("arg", arg);
		commandService.run(LegacyCommand.class, true, inputMap);
	}

	@Override
	public void syncActiveImage() {
		final ImageDisplay activeDisplay =
			imageDisplayService.getActiveImageDisplay();
		ij1Helper.syncActiveImage(activeDisplay);
	}

	@Override
	public boolean isInitialized() {
		return instance != null;
	}

	@Override
	public void syncColors() {
		final DatasetView view = imageDisplayService.getActiveDatasetView();
		if (view == null) return;
		final OptionsChannels channels = getChannels();
		final ColorRGB fgColor = view.getColor(channels.getFgValues());
		final ColorRGB bgColor = view.getColor(channels.getBgValues());
		optionsSynchronizer.colorOptions(fgColor, bgColor);
	}

	/**
	 * States whether we're running in legacy ImageJ 1.x mode.
	 * <p>
	 * To support work flows which are incompatible with ImageJ2, we want to allow
	 * users to run in legacy ImageJ 1.x mode, where the ImageJ2 GUI is hidden and
	 * the ImageJ 1.x GUI is shown. During this time, no synchronization should
	 * take place.
	 * </p>
	 */
	@Override
	public boolean isLegacyMode() {
		return ij1Helper != null && ij1Helper.isVisible();
	}

	/**
	 * Switch to/from running legacy ImageJ 1.x mode.
	 */
	@Override
	public void toggleLegacyMode(final boolean wantIJ1) {
		toggleLegacyMode(wantIJ1, false);
	}

	public synchronized void toggleLegacyMode(final boolean wantIJ1, final boolean initializing) {
		// TODO: hide/show Brightness/Contrast, Color Picker, Command Launcher, etc
		// TODO: prevent IJ1 from quitting without IJ2 quitting, too

		if (!initializing) {
			if (uiService != null) {
				// hide/show the IJ2 main window
				final UserInterface ui = uiService.getDefaultUI();
				final ApplicationFrame appFrame =
					ui == null ? null : ui.getApplicationFrame();
				if (appFrame == null) {
					if (ui != null && !wantIJ1) uiService.showUI();
				} else {
					appFrame.setVisible(!wantIJ1);
				}
			}

			// TODO: move this into the LegacyImageMap's toggleLegacyMode, passing
			// the uiService
			// hide/show the IJ2 datasets corresponding to legacy ImagePlus instances
			for (final ImageDisplay display : imageMap.getImageDisplays()) {
				final ImageDisplayViewer viewer =
					(ImageDisplayViewer) uiService.getDisplayViewer(display);
				if (viewer == null) continue;
				final DisplayWindow window = viewer.getWindow();
				if (window != null) window.showDisplay(!wantIJ1);
			}
		}

		// hide/show IJ1 main window
		ij1Helper.setVisible(wantIJ1);

		if (wantIJ1 && !initializing) {
			optionsSynchronizer.updateLegacyImageJSettingsFromModernImageJ();
		}

		imageMap.toggleLegacyMode(wantIJ1);
	}

	@Override
	public String getLegacyVersion() {
		return ij1Helper.getVersion();
	}

	// -- Service methods --

	@Override
	public void initialize() {
		checkInstance();

		ij1Helper = new IJ1Helper(this);
		boolean hasIJ1Instance = ij1Helper.hasInstance();

		imageMap = new LegacyImageMap(this);
		optionsSynchronizer = new OptionsSynchronizer(optionsService);

		synchronized (DefaultLegacyService.class) {
			checkInstance();
			instance = this;
			LegacyInjector.installHooks(getClass().getClassLoader(), new DefaultLegacyHooks(this, ij1Helper));
		}

		ij1Helper.initialize();

		SwitchToModernMode.registerMenuItem();

		addLegacyCompatibleCommands(menuService.getMenu());

		// discover legacy plugins
		final boolean enableBlacklist = true;
		addLegacyCommands(enableBlacklist);

		if (!hasIJ1Instance && !GraphicsEnvironment.isHeadless()) toggleLegacyMode(false, true);
	}

	// -- Package protected events processing methods --

	/**
	 * NB: This method is not intended for public consumption. It is really
	 * intended to be "jar protected". It is used to toggle a {@link ThreadLocal}
	 * flag as to whether or not legacy UI components are in the process of
	 * handling {@code StatusEvents}.
	 * <p>
	 * USE AT YOUR OWN RISK!
	 * </p>
	 *
	 * @return the old processing value
	 */
	public boolean setProcessingEvents(boolean processing) {
		boolean result = isProcessingEvents();
		if (result != processing) {
			isProcessingEvents.set(processing);
		}
		return result;
	}

	/**
	 * {@link ThreadLocal} check to see if components are in the middle of
	 * processing events.
	 * 
	 * @return True iff this thread is already processing events through the
	 *         {@code DefaultLegacyService}.
	 */
	public boolean isProcessingEvents() {
		Boolean result = isProcessingEvents.get();
		return result == Boolean.TRUE;
	}

	// -- Disposable methods --

	@Override
	public void dispose() {
		ij1Helper.dispose();

		LegacyInjector.installHooks(getClass().getClassLoader(), null);
		instance = null;
	}

	// -- Event handlers --

	/**
	 * Keeps the active legacy {@link ij.ImagePlus} in sync with the active modern
	 * {@link ImageDisplay}.
	 */
	@EventHandler
	protected void onEvent(final DisplayActivatedEvent event)
	{
		syncActiveImage();
	}

	@EventHandler
	protected void onEvent(final OptionsEvent event) {
		optionsSynchronizer.updateModernImageJSettingsFromLegacyImageJ();
	}

	@EventHandler
	protected void onEvent(final KyPressedEvent event) {
		final KeyCode code = event.getCode();
		if (code == KeyCode.SPACE) ij1Helper.setKeyDown(KeyCode.SPACE.getCode());
		if (code == KeyCode.ALT) ij1Helper.setKeyDown(KeyCode.ALT.getCode());
		if (code == KeyCode.SHIFT) ij1Helper.setKeyDown(KeyCode.SHIFT.getCode());
		if (code == KeyCode.CONTROL) ij1Helper.setKeyDown(KeyCode.CONTROL.getCode());
		if (ij1Helper.isMacintosh() && code == KeyCode.META) {
			ij1Helper.setKeyDown(KeyCode.CONTROL.getCode());
		}
	}

	@EventHandler
	protected void onEvent(final KyReleasedEvent event) {
		final KeyCode code = event.getCode();
		if (code == KeyCode.SPACE) ij1Helper.setKeyUp(KeyCode.SPACE.getCode());
		if (code == KeyCode.ALT) ij1Helper.setKeyUp(KeyCode.ALT.getCode());
		if (code == KeyCode.SHIFT) ij1Helper.setKeyUp(KeyCode.SHIFT.getCode());
		if (code == KeyCode.CONTROL) ij1Helper.setKeyUp(KeyCode.CONTROL.getCode());
		if (ij1Helper.isMacintosh() && code == KeyCode.META) {
			ij1Helper.setKeyUp(KeyCode.CONTROL.getCode());
		}
	}

	// -- pre-initialization

	/**
	 * Makes sure that the ImageJ 1.x classes are patched.
	 * <p>
	 * We absolutely require that the LegacyInjector did its job before we use the
	 * ImageJ 1.x classes.
	 * </p>
	 * <p>
	 * Just loading the {@link DefaultLegacyService} class is not enough; it will
	 * not necessarily get initialized. So we provide this method just to force
	 * class initialization (and thereby the LegacyInjector to patch ImageJ 1.x).
	 * </p>
	 * 
	 * @deprecated use {@link LegacyInjector#preinit()} instead
	 */
	public static void preinit() {
		LegacyInjector.preinit();
	}

	// -- helpers --

	/**
	 * Returns the legacy service associated with the ImageJ 1.x instance in the
	 * current class loader. This method is intended to be used by the
	 * {@link CodeHacker}; it is invoked by the javassisted methods.
	 * 
	 * @return the legacy service
	 */
	public static DefaultLegacyService getInstance() {
		return instance;
	}

	/**
	 * @throws UnsupportedOperationException if the singleton
	 *           {@code DefaultLegacyService} already exists.
	 */
	private void checkInstance() {
		if (instance != null) {
			throw new UnsupportedOperationException(
				"Cannot instantiate more than one DefaultLegacyService");
		}
	}

	private OptionsChannels getChannels() {
		return optionsService.getOptions(OptionsChannels.class);
	}

	@SuppressWarnings("unused")
	private void updateMenus(final boolean enableBlacklist) {
		pluginService.reloadPlugins();
		addLegacyCommands(enableBlacklist);
	}

	private void addLegacyCommands(final boolean enableBlacklist) {
		final LegacyPluginFinder finder =
			new LegacyPluginFinder(log, menuService.getMenu(), enableBlacklist);
		final ArrayList<PluginInfo<?>> plugins = new ArrayList<PluginInfo<?>>();
		finder.findPlugins(plugins);
		pluginService.addPlugins(plugins);
	}

	// -- Menu population --

	/**
	 * Adds all {@link LegacyCompatibleCommand}s in the provided
	 * {@link ShadowMenu} to the ImageJ1 menus. The nested menu structure of each
	 * {@code LegacyCompatibleCommand} is preserved.
	 */
	private void addLegacyCompatibleCommands(final ShadowMenu menu) {
		if (menu.getChildren().isEmpty()) {
			final Module m = moduleService.createModule(menu.getModuleInfo());
			if (m != null &&
				LegacyCompatibleCommand.class.isAssignableFrom(m.getClass()))
			{
				// Build menu hierarchy
				String[] menuHierarchy = new String[menu.getMenuDepth()];
				ShadowMenu parent = menu.getParent();

				for (int i = menuHierarchy.length - 1; i >= 0; i--) {
					menuHierarchy[i] = parent.getName();
					parent = parent.getParent();
				}

				@SuppressWarnings("unchecked")
				final Hashtable<String, String> commands = Menus.getCommands();
				if (!commands.containsKey(menu.getName())) {
					final MenuBar menuBar = Menus.getMenuBar();
					Menu ij1Menu = null;

					for (int i = 0; ij1Menu == null && i < menuBar.getMenuCount(); i++) {
						if (menuBar.getMenu(i).getLabel().equals(menuHierarchy[0])) {
							ij1Menu = menuBar.getMenu(i);
						}
					}

					// Create the desired IJ2 menu structure in IJ1
					ij1Menu = findOrCreateMenu(ij1Menu, menuHierarchy, 1);

					final MenuItem item = new MenuItem(menu.getName());
					item.addActionListener(this);
					// insert at the bottom of the IJ1 menu
					ij1Menu.insert(item, ij1Menu.getItemCount());

					commands.put(menu.getName(), m.getClass().getName());
				}
			}
		}
		else {
			for (final ShadowMenu child : menu.getChildren()) {
				addLegacyCompatibleCommands(child);
			}
		}
	}

	/**
	 * Given a base, parent {@code Menu}, this method will return the tail menu
	 * item based on the given {@code menuHierarchy}. If the hierarchy does not
	 * exist as specified, it will be built.
	 */
	private Menu findOrCreateMenu(final Menu menu, final String[] menuHierarchy,
		final int depth)
	{
		// if we reached the menu hierarchy depth, we can return our current menu.
		// This means that our desired IJ2 menu structure was exactly discovered
		// in the IJ1 menus.
		if (depth == menuHierarchy.length) {
			return menu;
		}

		// Check the current IJ1 menu to see if we have an entry that satisfies the
		// current depth of the IJ2 menu hierarchy
		for (int i = 0; i < menu.getItemCount(); i++) {
			final MenuItem menuItem = menu.getItem(i);
			if (menuItem.getLabel().equals(menuHierarchy[depth])) {
				// if a matching item is a Menu, we recurse
				if (menuItem instanceof Menu) {
					return findOrCreateMenu((Menu) menu.getItem(i), menuHierarchy,
						depth + 1);
				}
				// Otherwise, we mangle the menu hierarchy name as it is clashing with
				// an existing item in the IJ1 menu structure.
				menuHierarchy[depth] += "_IJ2";
			}
		}

		// We couldn't match the desired menu structure in existing IJ1 menus, so
		// we need to recreate the menu structure from the current depth onwards
		Menu expandedMenu = menu;
		for (int i=depth; i<menuHierarchy.length; i++) {
			Menu nextLevel = new Menu(menuHierarchy[depth], true);
			expandedMenu.add(nextLevel);
			expandedMenu = nextLevel;
		}

		return expandedMenu;
	}

	// -- ActionListener methods --

	/**
	 * Listen for IJ2 commands being selected from the IJ1 menu and run them
	 * using the {@link CommandService}.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (commandService != null && (e.getSource() instanceof MenuItem)) {
			final String cmd = e.getActionCommand();
			final String commandClass = (String)Menus.getCommands().get(cmd);
			commandService.run(commandClass, true, new Object[0]);
		}
	}
}
