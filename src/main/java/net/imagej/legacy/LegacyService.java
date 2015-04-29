/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
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

import net.imagej.ImageJService;
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

import org.scijava.app.StatusService;
import org.scijava.log.LogService;

/**
 * Interface for services that work with legacy ImageJ 1.x.
 * 
 * @author Barry DeZonia
 * @author Curtis Rueden
 */
@Plugin(type = Service.class, priority = Priority.NORMAL_PRIORITY + 1)
public final class LegacyService extends AbstractService {

	/**
	 * Static reference to the one and only active {@link LegacyService}. The JVM
	 * can only have one instance of ImageJ 1.x, and hence one LegacyService,
	 * active at a time.
	 */
	private static LegacyService instance;

	private static Throwable instantiationStackTrace;

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
	private ThresholdService thresholdService;

	/** Mapping between modern and legacy image data structures. */
	private LegacyImageMap imageMap;

	/**
	 * A buffer object which keeps all references to ImageJ 1.x separated from
	 * this class.
	 */
	private IJ1Helper ij1Helper;

	private final ThreadLocal<Boolean> isProcessingEvents =
		new ThreadLocal<Boolean>();

	/**
	 * Map of ImageJ2 {@link Command}s which are compatible with the legacy user
	 * interface. A command is considered compatible if it is not tagged with the
	 * {@code "no-legacy"} key in its {@link Parameter#attrs()} list. The map is
	 * keyed on identifier; see the {@link Identifiable} interface.
	 */
	private final Map<String, ModuleInfo> legacyCompatible =
		new HashMap<String, ModuleInfo>();

	// -- LegacyService methods --

>>>>>>> 07d32b2... fixup! LegacyService: group unused services together
	/** Gets the LogService associated with this LegacyService. */
	LogService log();

	/** Gets the StatusService associated with this LegacyService. */
	StatusService status();

	/** Gets the LegacyImageMap associated with this LegacyService. */
	LegacyImageMap getImageMap();

	/**
	 * Runs a legacy command programmatically.
	 * 
	 * @param ij1ClassName The name of the plugin class you want to run e.g.
	 *          "ij.plugin.Clipboard"
	 * @param argument The argument string to pass to the plugin e.g. "copy"
	 */
	void runLegacyCommand(String ij1ClassName, String argument);

	/**
	 * Ensures that the currently active {@link ij.ImagePlus} matches the
	 * currently active {@link ImageDisplay}. Does not perform any harmonization.
	 */
	void syncActiveImage();

	/**
	 * Returns true if this LegacyService has been initialized already and false
	 * if not.
	 */
	boolean isInitialized();

	/**
	 * States whether ImageJ1 and ImageJ2 data structures should be kept in sync.
	 * <p>
	 * While synchronization is supposed to be as cheap as possible, in practice
	 * there are limitations with it currently which impact performance. So
	 * such synchronization is off by default. The main consequence is that
	 * it becomes harder to "mix and match" ImageJ1 and ImageJ2 APIs: you cannot
	 * open an {@link ij.ImagePlus} and then reference it later from an ImageJ2
	 * {@link org.scijava.command.Command} as a {@link net.imagej.Dataset} unless
	 * synchronization is enabled.
	 */
	boolean isSyncEnabled();

	/**
	 * States whether we're running in legacy ImageJ 1.x mode.
	 * 
	 * To support work flows which are incompatible with ImageJ2, we want to allow
	 * users to run in legacy ImageJ 1.x mode, where the ImageJ2 GUI is hidden and
	 * the ImageJ 1.x GUI is shown. During this time, no synchronization should take
	 * place.
	 */
	boolean isLegacyMode();

	/**
	 * Switch to/from running legacy ImageJ 1.x mode.
	 */
	void toggleLegacyMode(boolean toggle);

	/** Gets the version of ImageJ 1.x being used. */
	String getLegacyVersion();

	/**
	 * Gets the combined version of ImageJ2/ImageJ1, with a slash separator.
	 * <p>
	 * This is the string that gets displayed in the ImageJ status bar.
	 * </p>
	 */
	String getCombinedVersion();

	void handleException(Throwable e);

}
