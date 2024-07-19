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

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import net.imagej.DatasetService;
import net.imagej.ImageJService;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.display.OverlayService;
import net.imagej.legacy.command.LegacyCommand;
import net.imagej.legacy.command.LegacyCommandFinder;
import net.imagej.legacy.command.LegacyCommandInfo;
import net.imagej.legacy.ui.LegacyUI;
import net.imagej.patcher.LegacyEnvironment;
import net.imagej.patcher.LegacyInjector;
import net.imagej.ui.viewer.image.ImageDisplayViewer;

import org.scijava.Context;
import org.scijava.Identifiable;
import org.scijava.MenuPath;
import org.scijava.Priority;
import org.scijava.UIDetails;
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
import org.scijava.module.event.ModuleCanceledEvent;
import org.scijava.module.event.ModuleFinishedEvent;
import org.scijava.module.event.ModuleStartedEvent;
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
import org.scijava.ui.swing.script.TextEditor;
import org.scijava.ui.viewer.DisplayWindow;
import org.scijava.util.AppUtils;

/**
 * Service for working with legacy ImageJ 1.x.
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
 * the service transparently translates it into an {@link ij.ImagePlus}, and
 * vice versa, enabling backward compatibility with legacy commands.
 * </p>
 *
 * @author Barry DeZonia
 * @author Curtis Rueden
 * @author Johannes Schindelin
 * @author Mark Hiner
 */
@Plugin(type = Service.class, priority = Priority.NORMAL + 1)
public final class LegacyService extends AbstractService implements
	ImageJService
{

	private static final String ENABLE_MODERN_ONLY_COMMANDS_PROPERTY =
		"imagej.legacy.modernOnlyCommands";

	/**
	 * Static reference to the one and only active {@link LegacyService}. The JVM
	 * can only have one instance of ImageJ 1.x, and hence one LegacyService,
	 * active at a time.
	 */
	private static LegacyService instance;

	static {
		// NB: Prime ImageJ 1.x for patching.
		// This will only work if this class does _not_ need to load any ij.*
		// classes for it itself to be loaded. I.e.: this class must have _no_
		// references to ij.* classes in its API (supertypes, fields, method
		// arguments and method return types).
		LegacyInjector.preinit();
	}

	@Parameter
	private LogService log;

	@Parameter
	private CommandService commandService;

	@Parameter
	private OptionsService optionsService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private ScriptService scriptService;

	@Parameter
	private StatusService statusService;

	@Parameter(required = false)
	private AppService appService;

	// FIXME: Why isn't this service declared as an optional parameter?
	private UIService uiService;

	// NB: Unused services, declared only to affect service initialization order.

	@Parameter(required = false)
	private DatasetService datasetService;

	@Parameter(required = false)
	private DisplayService displayService;

	@Parameter(required = false)
	private EventService eventService;

	@Parameter(required = false)
	private MenuService menuService;

	@Parameter(required = false)
	private OverlayService overlayService;

	@Parameter(required = false)
	private PluginService pluginService;

	@Parameter(required = false)
	@SuppressWarnings("deprecation")
	private net.imagej.threshold.ThresholdService thresholdService;

	/** Mapping between modern and legacy image data structures. */
	private LegacyImageMap imageMap;

	/**
	 * A buffer object which keeps all references to ImageJ 1.x separated from
	 * this class.
	 */
	private IJ1Helper ij1Helper;

	private final ThreadLocal<Boolean> isProcessingEvents = new ThreadLocal<>();

	/**
	 * Map of ImageJ2 {@link Command}s which are compatible with the legacy user
	 * interface. A command is considered compatible if it is not tagged with the
	 * {@code "no-legacy"} key in its {@link Parameter#attrs()} list. The map is
	 * keyed on identifier; see the {@link Identifiable} interface.
	 */
	private final Map<String, ModuleInfo> legacyCompatible = new HashMap<>();

	// -- LegacyService methods --

	/** Gets the LogService associated with this LegacyService. */
	@Override
	public LogService log() {
		return log;
	}

	/** Gets the StatusService associated with this LegacyService. */
	public StatusService status() {
		return statusService;
	}

	public synchronized UIService uiService() {
		if (uiService == null) uiService = getContext().getService(UIService.class);
		return uiService;
	}

	/**
	 * Gets whether this {@code LegacyService} is fully operational, linked to the
	 * ImageJ 1.x singleton instance. Inactive {@code LegacyService}s are dummies,
	 * which are not tied to ImageJ 1.x.
	 *
	 * @return true iff this {@code LegacyService} is the active one, tied to the
	 *         singleton instance of ImageJ 1.x.
	 */
	public boolean isActive() {
		return instance == this;
	}

	/**
	 * Gets the helper class responsible for direct interfacing with ImageJ1.
	 * Ideally, all accesses to {@code ij.*} classes should be done through this
	 * helper class, to avoid class loader woes.
	 *
	 * @return The {@link IJ1Helper}, or {@code null} if this
	 *         {@code LegacyService} is inactive (see {@link #isActive()}).
	 */
	public IJ1Helper getIJ1Helper() {
		return ij1Helper;
	}

	/** Gets the LegacyImageMap associated with this LegacyService. */
	public synchronized LegacyImageMap getImageMap() {
		if (!isActive()) return null;
		if (imageMap == null) imageMap = new LegacyImageMap(this);
		return imageMap;
	}

	/**
	 * Runs a legacy command programmatically.
	 *
	 * @param ij1ClassName The name of the plugin class you want to run e.g.
	 *          "ij.plugin.Clipboard"
	 * @param argument The argument string to pass to the plugin e.g. "copy"
	 */
	public void runLegacyCommand(final String ij1ClassName,
		final String argument)
	{
		checkActive();
		final String arg = argument == null ? "" : argument;
		final Map<String, Object> inputMap = new HashMap<>();
		inputMap.put("className", ij1ClassName);
		inputMap.put("arg", arg);
		commandService.run(LegacyCommand.class, true, inputMap);
	}

	/**
	 * Runs the legacy compatible command with the given identifier.
	 *
	 * @param key The identifier of the command to execute.
	 * @return The {@link Future} of the command execution; or if the identifier
	 *         describes a script and the shift key is down, then the
	 *         {@link TextEditor} of the new Script Editor window which was
	 *         opened.
	 * @see Identifiable
	 */
	public Object runLegacyCompatibleCommand(final String key) {
		checkActive();
		final ModuleInfo info = legacyCompatible.get(key);
		if (info == null) return null;
		if (info instanceof ScriptInfo) {
			if (ij1Helper.shiftKeyDown()) {
				// open the script in the script editor
				return openScriptInTextEditor((ScriptInfo) info);
			}
		}
		try {
			final Future<?> future = moduleService.run(info, true);
			return future == null ? null : future.get();
		}
		catch (final Exception e) {
			if (e instanceof RuntimeException) throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
	}

	/**
	 * Ensures that the currently active {@link ij.ImagePlus} matches the
	 * currently active {@link ImageDisplay}. Does not perform any harmonization.
	 */
	public void syncActiveImage() {
		if (!isActive()) return;
		final ImageDisplay activeDisplay = //
			imageDisplayService.getActiveImageDisplay();
		ij1Helper.syncActiveImage(activeDisplay);
	}

	/**
	 * Returns true if this LegacyService has been initialized already and false
	 * if not.
	 */
	public boolean isInitialized() {
		return instance != null;
	}

	/**
	 * States whether ImageJ1 and ImageJ2 data structures should be kept in sync.
	 * <p>
	 * While synchronization is supposed to be as cheap as possible, in practice
	 * there are limitations with it currently which impact performance. So such
	 * synchronization is off by default. The main consequence is that it becomes
	 * harder to "mix and match" ImageJ1 and ImageJ2 APIs: you cannot open an
	 * {@link ij.ImagePlus} and then reference it later from an ImageJ2
	 * {@link org.scijava.command.Command} as a {@link net.imagej.Dataset} unless
	 * synchronization is enabled.
	 */
	public boolean isSyncEnabled() {
		final ImageJ2Options ij2Options = //
			optionsService.getOptions(ImageJ2Options.class);
		return ij2Options == null ? false : ij2Options.isSyncEnabled();
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
	public boolean isLegacyMode() {
		return ij1Helper != null && ij1Helper.getIJ() != null;
	}

	/** Switches to/from running legacy ImageJ 1.x mode. */
	public void toggleLegacyMode(final boolean wantIJ1) {
		toggleLegacyMode(wantIJ1, false);
	}

	public synchronized void toggleLegacyMode(final boolean wantIJ1,
		final boolean initializing)
	{
		checkActive();

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
						log.error("Application frame missing: " + frame + " / " +
							modernFrame);
						return;
					}
					frame.setVisible(wantIJ1);
					modernFrame.setVisible(!wantIJ1);
				}
				else {
					final ApplicationFrame appFrame = //
						ui == null ? null : ui.getApplicationFrame();
					if (appFrame == null) {
						if (ui != null && !wantIJ1) uiService.showUI();
					}
					else {
						appFrame.setVisible(!wantIJ1);
					}
				}
			}

			// TODO: move this into the LegacyImageMap's toggleLegacyMode, passing
			// the uiService
			// hide/show the IJ2 datasets corresponding to legacy ImagePlus instances
			for (final ImageDisplay display : getImageMap().getImageDisplays()) {
				final ImageDisplayViewer viewer = //
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

	public App getApp() {
		if (appService == null) return null;
		return appService.getApp();
	}

	public void handleException(final Throwable e) {
		log.error(e);
		if (ij1Helper != null) ij1Helper.handleException(e);
	}

	// -- Service methods --

	@Override
	public void initialize() {
		if (instance != null) {
			// NB: There is already an active LegacyService. So this one is a dummy,
			// part of another simultaneously existing application context.
			return;
		}
		synchronized (LegacyService.class) {
			if (instance != null) return; // double-checked locking
			try {
				// Install the default legacy hooks before ImageJ 1.x initializes.
				// Otherwise, the legacy hooks that fire during IJ1 initialization
				// won't include DefaultLegacyHooks overrides of EssentialLegacyHooks.
				final ClassLoader loader = Context.getClassLoader();
				ij1Helper = new IJ1Helper(this);
				LegacyInjector.installHooks(loader, //
					new DefaultLegacyHooks(this));
				instance = this;

				// Initialize ImageJ 1.x, if needed.
				final boolean ij1Initialized = //
					LegacyEnvironment.isImageJ1Initialized(loader);
				if (!ij1Initialized) getLegacyEnvironment(loader).newImageJ1(true);
			}
			catch (final Throwable t) {
				log.error("Failed to instantiate IJ1.", t);
				return;
			}
		}

		ij1Helper.initialize();
		ij1Helper.addAliases(scriptService);

		// NB: We cannot call appService.getApp().getBaseDirectory(), because
		// that prevents the net.imagej.app.ToplevelImageJApp from getting its
		// LegacyService parameter injected properly.
		// So we get the app directory in a much more unsafe way...
		final File topLevel = //
			AppUtils.getBaseDirectory("imagej.dir", getClass(), null);

		final File plugins = new File(topLevel, "plugins");
		if (plugins.exists()) {
			final File scripts = new File(plugins, "Scripts");
			if (scripts.exists()) scriptService.addScriptDirectory(scripts);
			scriptService.addScriptDirectory(plugins, new MenuPath("Plugins"));
		}

		// remove modules blocklisted from the legacy UI
		if (Boolean.getBoolean(ENABLE_MODERN_ONLY_COMMANDS_PROPERTY)) {
			log.info("Skipping blocklist of no-legacy commands");
		}
		else {
			final List<ModuleInfo> noLegacyModules = //
				moduleService.getModules().stream() //
					.filter(info -> info.is("no-legacy")) //
					.collect(Collectors.toList());
			moduleService.removeModules(noLegacyModules);
		}

		// wrap ImageJ 1.x commands as SciJava modules
		final List<CommandInfo> ij1Commands = //
			new LegacyCommandFinder(this).findCommands();

		ij1Helper.addMenuItems();

		// register ImageJ 1.x modules with the module service.
		moduleService.addModules(ij1Commands);
	}

	// -- Disposable methods --

	@Override
	public void dispose() {
		if (!isActive()) return;

		ij1Helper.dispose();

		synchronized (LegacyService.class) {
			final ClassLoader loader = Context.getClassLoader();
			LegacyInjector.installHooks(loader, null);
			instance = null;
		}

		// clean up SingleInstance remote objects
		SingleInstance.shutDown();
	}

	// -- Versioned methods --

	/** Gets the version of ImageJ 1.x being used. */
	@Override
	public String getVersion() {
		return ij1Helper == null ? "Inactive" : ij1Helper.getVersion();
	}

	// -- Utility methods --

	/**
	 * Returns the legacy service associated with the ImageJ 1.x instance in the
	 * current class loader. This method is invoked by the javassisted methods of
	 * ImageJ 1.x.
	 *
	 * @return the legacy service
	 */
	public static LegacyService getInstance() {
		return instance;
	}

	// -- Event handlers --

	/**
	 * Keeps the active legacy {@link ij.ImagePlus} in sync with the active modern
	 * {@link ImageDisplay}.
	 *
	 * @param event
	 */
	@EventHandler
	private void onEvent(final DisplayActivatedEvent event) {
		syncActiveImage();
	}

	@EventHandler
	private void onEvent(final KyPressedEvent event) {
		if (!isActive()) return;
		final KeyCode code = event.getCode();
		if (code == KeyCode.SPACE) ij1Helper.setKeyDown(KeyCode.SPACE.getCode());
		if (code == KeyCode.ALT) ij1Helper.setKeyDown(KeyCode.ALT.getCode());
		if (code == KeyCode.SHIFT) ij1Helper.setKeyDown(KeyCode.SHIFT.getCode());
		if (code == KeyCode.CONTROL) {
			ij1Helper.setKeyDown(KeyCode.CONTROL.getCode());
		}
		if (ij1Helper.isMacintosh() && code == KeyCode.META) {
			ij1Helper.setKeyDown(KeyCode.CONTROL.getCode());
		}
	}

	@EventHandler
	private void onEvent(final KyReleasedEvent event) {
		if (!isActive()) return;
		final KeyCode code = event.getCode();
		if (code == KeyCode.SPACE) ij1Helper.setKeyUp(KeyCode.SPACE.getCode());
		if (code == KeyCode.ALT) ij1Helper.setKeyUp(KeyCode.ALT.getCode());
		if (code == KeyCode.SHIFT) ij1Helper.setKeyUp(KeyCode.SHIFT.getCode());
		if (code == KeyCode.CONTROL) ij1Helper.setKeyUp(KeyCode.CONTROL.getCode());
		if (ij1Helper.isMacintosh() && code == KeyCode.META) {
			ij1Helper.setKeyUp(KeyCode.CONTROL.getCode());
		}
	}

	@EventHandler
	private void onEvent(final ModuleStartedEvent evt) {
		Macros.setActiveModule(evt.getModule());
	}

	@EventHandler
	private void onEvent(@SuppressWarnings("unused") final ModuleCanceledEvent evt) {
		Macros.setActiveModule(null);
	}

	@EventHandler
	private void onEvent(@SuppressWarnings("unused") final ModuleFinishedEvent evt) {
		Macros.setActiveModule(null);
	}

	// -- Internal methods --

	/**
	 * <strong>This is not part of the public API. DO NOT USE!</strong>
	 * <p>
	 * This method makes it possible to override the {@link IJ1Helper} behavior,
	 * to facilitate unit testing.
	 * </p>
	 */
	public void setIJ1Helper(final IJ1Helper ij1Helper) {
		this.ij1Helper = ij1Helper;
	}

	/**
	 * <strong>This is not part of the public API. DO NOT USE!</strong>
	 * <p>
	 * This method toggles a {@link ThreadLocal} flag as to whether or not legacy
	 * UI components are in the process of handling {@code StatusEvents}.
	 * </p>
	 *
	 * @return the old processing value
	 */
	public boolean setProcessingEvents(final boolean processing) {
		final boolean result = isProcessingEvents();
		if (result != processing) {
			isProcessingEvents.set(processing);
		}
		return result;
	}

	/**
	 * <strong>This is not part of the public API. DO NOT USE!</strong>
	 * <p>
	 * {@link ThreadLocal} check to see if components are in the middle of
	 * processing events.
	 * </p>
	 *
	 * @return True iff this thread is already processing events through the
	 *         {@code LegacyService}.
	 */
	public boolean isProcessingEvents() {
		final Boolean result = isProcessingEvents.get();
		return result == Boolean.TRUE;
	}

	/**
	 * <strong>This is not part of the public API. DO NOT USE!</strong>
	 * <p>
	 * Adds all legacy compatible commands to the ImageJ1 menus. The nested menu
	 * structure of each command is preserved.
	 * </p>
	 */
	public Map<String, ModuleInfo> getScriptsAndNonLegacyCommands() {
		final Map<String, ModuleInfo> modules = new LinkedHashMap<>();
		legacyCompatible.clear();
		for (final CommandInfo info : commandService.getCommandsOfType(
			Command.class))
		{
			if (!UIDetails.APPLICATION_MENU_ROOT.equals(info.getMenuRoot()) || //
				info.getMenuPath().size() == 0 || info.is("no-legacy") || //
				!info.getAnnotation().visible())
			{
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

	/** <strong>This is not part of the public API. DO NOT USE!</strong> */
	boolean handleShortcut(final String accelerator) {
		final Accelerator acc = Accelerator.create(accelerator);
		if (acc == null) return false;
		final ModuleInfo module = moduleService.getModuleForAccelerator(acc);

		// NB: ImageJ 1.x handles its own keyboard shortcuts.
		// We need to ignore legacy commands to avoid duplicate execution.
		// See: https://github.com/imagej/imagej1/issues/50
		if (module == null || module instanceof LegacyCommandInfo) return false;

		moduleService.run(module, true);
		return true;
	}

	// -- Helper methods --

	/**
	 * @throws UnsupportedOperationException if this {@code LegacyService} is not
	 *           the active one.
	 * @see #isActive()
	 */
	private void checkActive() {
		if (isActive()) return;
		throw new UnsupportedOperationException(
			"This context's LegacyService is inactive");
	}

	public TextEditor openScriptInTextEditor(final ScriptInfo script) {
		final TextEditor editor = new TextEditor(getContext());

		final File scriptFile = getScriptFile(script);
		if (scriptFile.exists()) {
			// script is a file on disk; open it
			editor.open(scriptFile);
			editor.setVisible(true);
			return editor;
		}

		// try to read the script from its associated reader
		final StringBuilder sb = new StringBuilder();
		try (final BufferedReader reader = script.getReader()) {
			if (reader != null) {
				// script is text from somewhere (a URL?); read it
				while (true) {
					final String line = reader.readLine();
					if (line == null) break; // eof
					sb.append(line);
					sb.append("\n");
				}
			}
		}
		catch (final IOException exc) {
			log.error("Error reading script: " + script.getPath(), exc);
		}

		if (sb.length() > 0) {
			// script came from somewhere, but not from a regular file
			editor.getEditorPane().setFileName(scriptFile);
			editor.getEditorPane().setText(sb.toString());
		}
		else {
			// give up, and report the problem
			final String error = "[Cannot load script: " + script.getPath() + "]";
			editor.getEditorPane().setText(error);
		}

		editor.setVisible(true);
		return editor;
	}

	private File getScriptFile(final ScriptInfo script) {
		final URL scriptURL = script.getURL();
		try {
			if (scriptURL != null) return new File(scriptURL.toURI());
		}
		catch (final URISyntaxException | IllegalArgumentException exc) {
			log.debug(exc);
		}
		final File scriptDir = scriptService.getScriptDirectories().get(0);
		return new File(scriptDir.getPath() + File.separator + script.getPath());
	}

	private static LegacyEnvironment getLegacyEnvironment(
		final ClassLoader loader) throws ClassNotFoundException
	{
		final boolean headless = GraphicsEnvironment.isHeadless();
		final LegacyEnvironment ij1 = new LegacyEnvironment(loader, headless);
		ij1.disableInitializer();
		ij1.noPluginClassLoader();
		ij1.suppressIJ1ScriptDiscovery();
		ij1.applyPatches();
		return ij1;
	}

	// -- Deprecated methods --

	/**
	 * Makes sure that the ImageJ 1.x classes are patched.
	 * <p>
	 * We absolutely require that the LegacyInjector did its job before we use the
	 * ImageJ 1.x classes.
	 * </p>
	 * <p>
	 * Just loading the {@link LegacyService} class is not enough; it will not
	 * necessarily get initialized. So we provide this method just to force class
	 * initialization (and thereby the LegacyInjector to patch ImageJ 1.x).
	 * </p>
	 *
	 * @deprecated use {@link LegacyInjector#preinit()} instead
	 */
	@Deprecated
	public static void preinit() {
		try {
			getLegacyEnvironment(Context.getClassLoader());
		}
		catch (final Throwable t) {
			t.printStackTrace();
		}
	}

}
