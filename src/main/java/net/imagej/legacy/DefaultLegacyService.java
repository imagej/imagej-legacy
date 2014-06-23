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

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.imagej.DatasetService;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;
import net.imagej.legacy.plugin.LegacyCommand;
import net.imagej.legacy.ui.LegacyUI;
import net.imagej.patcher.LegacyEnvironment;
import net.imagej.patcher.LegacyInjector;
import net.imagej.threshold.ThresholdService;
import net.imagej.ui.swing.script.TextEditor;
import net.imagej.ui.viewer.image.ImageDisplayViewer;

import org.scijava.MenuPath;
import org.scijava.Priority;
import org.scijava.app.App;
import org.scijava.app.AppService;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.display.event.DisplayActivatedEvent;
import org.scijava.display.event.input.KyPressedEvent;
import org.scijava.display.event.input.KyReleasedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.input.Accelerator;
import org.scijava.input.KeyCode;
import org.scijava.log.LogService;
import org.scijava.menu.MenuService;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.ui.ApplicationFrame;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.welcome.event.WelcomeEvent;

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
@Plugin(type = Service.class, priority = Priority.NORMAL_PRIORITY + 1)
public final class DefaultLegacyService extends AbstractService implements
	LegacyService
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
	private ScriptService scriptService;

	@Parameter
	private StatusService statusService;

	@Parameter(required = false)
	private AppService appService;

	private UIService uiService;

	private static DefaultLegacyService instance;
	private static Throwable instantiationStackTrace;

	/** Mapping between modern and legacy image data structures. */
	private LegacyImageMap imageMap;

	/** Keep references to ImageJ 1.x separate */
	private IJ1Helper ij1Helper;

	// FIXME: See https://github.com/imagej/imagej-legacy/issues/53
	public IJ1Helper getIJ1Helper() {
		return ij1Helper;
	}

	private ThreadLocal<Boolean> isProcessingEvents = new ThreadLocal<Boolean>();

	private Map<String, ModuleInfo> legacyCompatible = new HashMap<String, ModuleInfo>();

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
	public synchronized LegacyImageMap getImageMap() {
		if (imageMap == null) imageMap = new LegacyImageMap(this);
		return imageMap;
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

	public Object runLegacyCompatibleCommand(final String key) {
		final ModuleInfo info = legacyCompatible.get(key);
		if (info == null) return null;
		if (info instanceof CommandInfo) try {
			return commandService.run((CommandInfo) info, true).get();
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
		if (info instanceof ScriptInfo) {
			if (ij1Helper.shiftKeyDown()) {
				final ScriptInfo script = (ScriptInfo) info;
				final TextEditor editor = new TextEditor(getContext());
				editor.open(new File(script.getPath()));
				editor.setVisible(true);
				return editor;
			}
			try {
				return scriptService.run((ScriptInfo) info, true).get();
			}
			catch (Exception e) {
				if (e instanceof RuntimeException) throw (RuntimeException) e;
				throw new RuntimeException(e);
			}
		}
		throw new IllegalArgumentException("Unhandled info for '" + key + "': " + info);
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
	public boolean isLegacyMode() {
		return ij1Helper != null && ij1Helper.isVisible();
	}

	@Override
	public void toggleLegacyMode(final boolean wantIJ1) {
		toggleLegacyMode(wantIJ1, false);
	}

	public synchronized void toggleLegacyMode(final boolean wantIJ1, final boolean initializing) {
		// TODO: hide/show Brightness/Contrast, Color Picker, Command Launcher, etc

		if (!initializing) {
			if (uiService() != null) {
				// hide/show the IJ2 main window
				final UserInterface ui = uiService.getDefaultUI();
				if (ui != null && ui instanceof LegacyUI) {
					UserInterface modern = null;
					for (final UserInterface ui2 : uiService.getAvailableUIs()) {
						if (ui2 == ui) continue;
						modern = ui2;
						break;
					}
					if (modern == null) {
						log.error("No modern UI available");
						return;
					}
					final ApplicationFrame frame = ui.getApplicationFrame();
					ApplicationFrame modernFrame = modern.getApplicationFrame();
					if (!wantIJ1 && modernFrame == null) {
						if (ij1Helper.isVisible()) modern.show();
						modernFrame = modern.getApplicationFrame();
					}
					if (frame == null || modernFrame == null) {
						log.error("Application frame missing: " + frame + " / " + modernFrame);
						return;
					}
					frame.setVisible(wantIJ1);
					modernFrame.setVisible(!wantIJ1);
				} else {
					final ApplicationFrame appFrame =
						ui == null ? null : ui.getApplicationFrame();
					if (appFrame == null) {
						if (ui != null && !wantIJ1) uiService.showUI();
					} else {
						appFrame.setVisible(!wantIJ1);
					}
				}
			}

			// TODO: move this into the LegacyImageMap's toggleLegacyMode, passing
			// the uiService
			// hide/show the IJ2 datasets corresponding to legacy ImagePlus instances
			for (final ImageDisplay display : getImageMap().getImageDisplays()) {
				final ImageDisplayViewer viewer =
					(ImageDisplayViewer) uiService.getDisplayViewer(display);
				if (viewer == null) continue;
				final DisplayWindow window = viewer.getWindow();
				if (window != null) window.showDisplay(!wantIJ1);
			}
		}

		// hide/show IJ1 main window
		ij1Helper.setVisible(wantIJ1);

		getImageMap().toggleLegacyMode(wantIJ1);
	}

	@Override
	public String getLegacyVersion() {
		return ij1Helper.getVersion();
	}

	// -- Service methods --

	@Override
	public void initialize() {
		checkInstance();

		try {
			final ClassLoader loader = Thread.currentThread().getContextClassLoader();
			final boolean ij1Initialized = LegacyEnvironment.isImageJ1Initialized(loader);
			if (!ij1Initialized) {
				getLegacyEnvironment(loader).newImageJ1(true);
			}
			ij1Helper = new IJ1Helper(this);
		} catch (final Throwable t) {
			throw new RuntimeException("Failed to instantiate IJ1.", t);
		}

		synchronized (DefaultLegacyService.class) {
			checkInstance();
			instance = this;
			instantiationStackTrace = new Throwable("Initialized here:");
			final ClassLoader loader = Thread.currentThread().getContextClassLoader();
			LegacyInjector.installHooks(loader, new DefaultLegacyHooks(this, ij1Helper));
		}

		ij1Helper.initialize();
		ij1Helper.addAliases(scriptService);

		SwitchToModernMode.registerMenuItem();

		final File topLevel = appService.getApp().getBaseDirectory();
		final File plugins = new File(topLevel, "plugins");
		if (plugins.exists()) {
			final File scripts = new File(plugins, "Scripts");
			if (scripts.exists()) scriptService.addScriptDirectory(scripts);
			scriptService.addScriptDirectory(plugins, new MenuPath("Plugins"));
		}

		ij1Helper.addMenuItems();
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

		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		LegacyInjector.installHooks(loader, null);
		synchronized(DefaultLegacyService.class) {
			instance = null;
			instantiationStackTrace = null;
		}
	}

	// -- Event handlers --

	/**
	 * Keeps the active legacy {@link ij.ImagePlus} in sync with the active modern
	 * {@link ImageDisplay}.
	 * 
	 * @param event
	 */
	@EventHandler
	private void onEvent(final DisplayActivatedEvent event)
	{
		syncActiveImage();
	}

	@EventHandler
	private void onEvent(final KyPressedEvent event) {
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
	private void onEvent(final KyReleasedEvent event) {
		final KeyCode code = event.getCode();
		if (code == KeyCode.SPACE) ij1Helper.setKeyUp(KeyCode.SPACE.getCode());
		if (code == KeyCode.ALT) ij1Helper.setKeyUp(KeyCode.ALT.getCode());
		if (code == KeyCode.SHIFT) ij1Helper.setKeyUp(KeyCode.SHIFT.getCode());
		if (code == KeyCode.CONTROL) ij1Helper.setKeyUp(KeyCode.CONTROL.getCode());
		if (ij1Helper.isMacintosh() && code == KeyCode.META) {
			ij1Helper.setKeyUp(KeyCode.CONTROL.getCode());
		}
	}

	/** @param event */
	@EventHandler
	private void onEvent(final WelcomeEvent event) {
		commandService.run(ImageJ2Options.class, true);
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
	@Deprecated
	public static void preinit() {
		try {
			getLegacyEnvironment(Thread.currentThread().getContextClassLoader());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private static LegacyEnvironment
		getLegacyEnvironment(final ClassLoader loader)
			throws ClassNotFoundException
	{
		final boolean headless = GraphicsEnvironment.isHeadless();
		final LegacyEnvironment ij1 = new LegacyEnvironment(loader, headless);
		ij1.disableInitializer();
		ij1.noPluginClassLoader();
		ij1.suppressIJ1ScriptDiscovery();
		ij1.applyPatches();
		return ij1;
	}

	// -- helpers --

	/**
	 * Returns the legacy service associated with the ImageJ 1.x instance in the
	 * current class loader. This method is invoked by the javassisted methods of
	 * ImageJ 1.x.
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
				"Cannot instantiate more than one DefaultLegacyService", instantiationStackTrace);
		}
	}

	public App getApp() {
		if (appService == null) return null;
		return appService.getApp();
	}

	// -- Menu population --

	/**
	 * Adds all legacy compatible commands to the ImageJ1 menus. The nested menu
	 * structure of each command is preserved.
	 * <p>
	 * This is not part of the public API. DO-NOT-USE!
	 * </p>
	 */
	public Map<String, ModuleInfo> getScriptsAndNonLegacyCommands() {
		final Map<String, ModuleInfo> modules = new LinkedHashMap<String, ModuleInfo>();
		legacyCompatible.clear();
		for (final CommandInfo info : commandService.getCommandsOfType(Command.class)) {
			if (info.getMenuPath().size() == 0 || info.is("no-legacy")) {
				continue;
			}
			else if (!info.getAnnotation().visible()) {
				continue;
			}
			final String key = info.getIdentifier();
			legacyCompatible.put(key, info);
			modules.put(key, info);
		}
		for (final ScriptInfo info : scriptService.getScripts()) {
			if (info.getMenuPath().size() == 0) {
				continue;
			}
			final String path = info.getPath();
			if (!new File(path).getName().contains("_")) continue;
			final String key = info.getIdentifier();
			legacyCompatible.put(key, info);
			modules.put(key, info);
		}
		return modules;
	}

	boolean handleShortcut(final String accelerator) {
		final Accelerator acc = Accelerator.create(accelerator);
		if (acc == null) return false;
		final ModuleInfo module = moduleService.getModuleForAccelerator(acc);
		if (module == null || module.is("no-legacy")) return false;
		moduleService.run(module, true);
		return true;
	}

	public synchronized UIService uiService() {
		if (uiService == null) uiService = getContext().getService(UIService.class);
		return uiService;
	}

	@Override
	public void handleException(Throwable e) {
		log.error(e);
		ij1Helper.handleException(e);
	}
}
