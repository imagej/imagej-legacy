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

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Roi;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.translate.DefaultImageTranslator;
import net.imagej.legacy.translate.Harmonizer;
import net.imagej.legacy.translate.ImageTranslator;
import net.imagej.legacy.translate.LegacyUtils;
import net.imagej.overlay.Overlay;
import net.imagej.patcher.LegacyInjector;
import net.imagej.ui.viewer.image.ImageDisplayViewer;

import org.scijava.AbstractContextual;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.display.event.DisplayDeletedEvent;
import org.scijava.display.event.DisplayUpdatedEvent;
import org.scijava.event.EventHandler;
import org.scijava.plugin.Parameter;
import org.scijava.ui.viewer.DisplayWindow;

/**
 * An image map between legacy ImageJ {@link ImagePlus} objects and modern
 * ImageJ {@link ImageDisplay}s. Because every {@link ImagePlus} has a
 * corresponding {@link ImageWindow} and vice versa, it works out best to
 * associate each {@link ImagePlus} with a {@link ImageDisplay} rather than with
 * a {@link Dataset}.
 * <p>
 * Any {@link Overlay}s present in the {@link ImageDisplay} are translated to a
 * {@link Roi} attached to the {@link ImagePlus}, and vice versa.
 * </p>
 * <p>
 * In the case of one {@link Dataset} belonging to multiple {@link ImageDisplay}
 * s, there is a separate {@link ImagePlus} for each {@link ImageDisplay}, with
 * pixels by reference.
 * </p>
 * <p>
 * In the case of multiple {@link Dataset}s in a single {@link ImageDisplay},
 * only the first {@link Dataset} is translated to the {@link ImagePlus}.
 * </p>
 * 
 * @author Curtis Rueden
 * @author Barry DeZonia
 */
public class LegacyImageMap extends AbstractContextual {

	/**
	 * Key for storing {@link ImagePlus} instances in a {@link Dataset}'s map.
	 */
	public static final String IMP_KEY = "ij1-image-plus";

	static {
		/*
		 * We absolutely require that the LegacyInjector did its job before we
		 * use the ImageJ 1.x classes here, just in case somebody wants to use
		 * the LegacyService later (and hence requires the ImageJ 1.x classes to
		 * be patched appropriately).
		 * 
		 * Just loading the class is not enough; it will not get initialized. So
		 * we call the preinit() method just to force class initialization (and
		 * thereby the LegacyInjector to patch ImageJ 1.x).
		 */
		LegacyInjector.preinit();
	}

	// -- Fields --

	/**
	 * Table of shadowing {@link ImagePlus} objects corresponding to
	 * {@link ImageDisplay}s created in modern mode. This table should not be used
	 * to track {@code ImagePlus} instances created in legacy mode - instead, use
	 * {@link #legacyImagePlusTable}.
	 */
	private final Map<ImageDisplay, ImagePlus> imagePlusTable;

	/**
	 * Table of {@link ImageDisplay} objects created in modern mode corresponding
	 * to shadowing {@link ImagePlus}es. This table should not be used to track
	 * {@code ImagePlus} instances created in legacy mode - instead, use
	 * {@link #legacyDisplayTable}.
	 */
	private final Map<ImagePlus, ImageDisplay> displayTable;

	/**
	 * A mapping of {@link ImagePlus} instances created in legacy mode to
	 * shadowing {@link ImageDisplay} instances. Uses a {@link WeakHashMap} so any
	 * {@code ImageDisplays} are disposed when the {@code ImagePlus} key is
	 * garbage collected - but maintains hard references to the
	 * {@code ImageDisplay}s otherwise.
	 */
	private final Map<ImagePlus, ImageDisplay> legacyDisplayTable =
		new WeakHashMap<>();

	/**
	 * Legacy mode mapping of {@link ImageDisplay}s to {@link ImagePlus}es. Uses
	 * {@link WeakReference}s for both keys and values.
	 */
	private final Map<ImageDisplay, WeakReference<ImagePlus>> legacyImagePlusTable =
		new WeakHashMap<>();

	/**
	 * Effectively a {@code WeakHashSet} for tracking known {@link ImagePlus}es.
	 */
	private final Map<ImagePlus, Object> imagePluses =
		new WeakHashMap<>();

	/**
	 * The {@link ImageTranslator} to use when creating {@link ImagePlus} and
	 * {@link ImageDisplay} objects corresponding to one another.
	 */
	private final DefaultImageTranslator imageTranslator;

	/**
	 * The legacy service corresponding to this image map.
	 */
	private final LegacyService legacyService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private DisplayService displayService;

	// -- Constructor --

	public LegacyImageMap(final LegacyService legacyService) {
		setContext(legacyService.getContext());
		this.legacyService = legacyService;
		imagePlusTable = new ConcurrentHashMap<>();
		displayTable = new ConcurrentHashMap<>();
		imageTranslator = new DefaultImageTranslator(legacyService);
	}

	// -- LegacyImageMap methods --

	/**
	 * Gets the {@link ImageDisplay} corresponding to the given {@link ImagePlus},
	 * or null if there is no existing table entry.
	 */
	public ImageDisplay lookupDisplay(final ImagePlus imp) {
		if (imp == null) return null;
		if (legacyService.isLegacyMode()) return legacyDisplayTable.get(imp);
		return displayTable.get(imp);
	}

	/**
	 * Gets the {@link ImagePlus} corresponding to the given {@link ImageDisplay},
	 * or null if there is no existing table entry.
	 */
	public ImagePlus lookupImagePlus(final ImageDisplay display) {
		if (display == null) return null;
		if (legacyService.isLegacyMode()) {
			final WeakReference<ImagePlus> weakReference =
				legacyImagePlusTable.get(display);
			return weakReference == null ? null : weakReference.get();
		}
		return imagePlusTable.get(display);
	}

	/**
	 * This method takes a provided {@link Dataset}, converts it to an
	 * {@link ImagePlus}, stores the new {@code ImagePlus} in the {@code Dataset}
	 * 's properties uner {@link LegacyImageMap#IMP_KEY} and finally creates an
	 * {@link ImageDisplay} using the provided {@code Dataset}. This display will
	 * not be rendered due to the {@code IMP_KEY} mapping. The resulting
	 * {@code ImagePlus} and {@code ImageDisplay} will then be mapped to each
	 * other.
	 * <p>
	 * Use this method to create an {@code ImagePlus} to {@code Display} mapping
	 * without rendering the display.
	 * </p>
	 *
	 * @return the {@link ImagePlus} object shadowing the given {@link Dataset}.
	 */
	public ImagePlus registerDataset(final Dataset ds) {
		final ImagePlus imp = imageTranslator.createLegacyImage(ds);
		ds.getProperties().put(LegacyImageMap.IMP_KEY, imp);
		final ImageDisplay display =
			(ImageDisplay)displayService.createDisplay(ds.getName(), ds);
		addMapping(display, imp);
		return imp;
	}

	/**
	 * Ensures that the given {@link ImageDisplay} has a corresponding legacy
	 * image.
	 * 
	 * @return the {@link ImagePlus} object shadowing the given
	 *         {@link ImageDisplay}, creating it if necessary using the
	 *         {@link ImageTranslator}.
	 */
	public ImagePlus registerDisplay(final ImageDisplay display) {
		return registerDisplay(display, legacyService.isLegacyMode());
	}

	/**
	 * As {@link #registerDisplay(ImageDisplay)} but mappings will go in legacy
	 * maps if {@code createLegacyMappings} is true.
	 */
	public ImagePlus registerDisplay(final ImageDisplay display,
		final boolean createLegacyMappings)
	{
		ImagePlus imp = lookupImagePlus(display);
		if (imp == null) {
			// mapping does not exist; mirror display to image window
			imp = imageTranslator.createLegacyImage(display);
			addMapping(display, imp, createLegacyMappings);
		}
		return imp;
	}

	/**
	 * Ensures that the given legacy image has a corresponding
	 * {@link ImageDisplay}.
	 * 
	 * @return the {@link ImageDisplay} object shadowing the given
	 *         {@link ImagePlus}, creating it if necessary using the
	 *         {@link ImageTranslator}.
	 */
	public ImageDisplay registerLegacyImage(final ImagePlus imp) {
		ImageDisplay display = lookupDisplay(imp);
		// It is possible that this method can get hit multiple times from the
		// display that is being created by the imageTranslator. Thus we want to
		// avoid an infinite loop.
		if (display == null && !imagePluses.containsKey(imp)) {
			imagePluses.put(imp, null);
			// mapping does not exist; mirror legacy image to display
			display = imageTranslator.createDisplay(imp);
			addMapping(display, imp);
	
			// record resultant ImagePlus as a legacy command output
			LegacyOutputTracker.addOutput(imp);
		}
	
		return display;
	}

	public synchronized void toggleLegacyMode(boolean enteringLegacyMode) {
		final Harmonizer harmonizer =
			new Harmonizer(legacyService.getContext(), imageTranslator);
		if (enteringLegacyMode) {
			// migrate from the ImagePlusTable and DisplayTable to legacy versions.
			final List<ImageDisplay> imageDisplays =
					imageDisplayService.getImageDisplays();
			// TODO: this is almost exactly what LegacyCommand does, so it is
			// pretty obvious that it is misplaced in there.
			for (final ImageDisplay display : imageDisplays) {
				ImagePlus imp = lookupImagePlus(display);
				if (imp == null) {
					final Dataset ds = imageDisplayService.getActiveDataset(display);
					if (LegacyUtils.dimensionsIJ1Compatible(ds)) {
						// Ensure the mappings are registered in the legacy maps
						imp = registerDisplay(display, true);
						final ImageDisplayViewer viewer =
								(ImageDisplayViewer) legacyService.uiService().getDisplayViewer(display);
						if (viewer != null) {
							final DisplayWindow window = viewer.getWindow();
							if (window != null) window.showDisplay(!enteringLegacyMode);
						}
					}
				}
				else {
					imp.unlock();
				}
				harmonizer.updateLegacyImage(display, imp);
				harmonizer.registerType(imp);
			}
			imagePlusTable.clear();
			displayTable.clear();
		}
		else {
			// migrate from legacyImagePlusTable and legacyDisplayTable to modern
			// versions.
			for (final ImagePlus imp : legacyDisplayTable.keySet()) {
				final ImageWindow window = imp.getWindow();
				final ImageDisplay display = legacyDisplayTable.get(imp);
				if (window == null || window.isClosed()) {
					// This ImagePlus was closed, so we can remove it from our mappings
					unregisterLegacyImage(imp);
					display.close();
				}
				else {
					// transfer mappings to modern maps with hard references
					displayTable.put(imp, display);
					imagePlusTable.put(display, imp);
					// Update the display
					harmonizer.updateDisplay(display, imp);
				}
			}
			legacyDisplayTable.clear();
			legacyImagePlusTable.clear();
		}
	}

	/** Removes the mapping associated with the given {@link ImageDisplay}. */
	public void unregisterDisplay(final ImageDisplay display) {
		unregisterDisplay(display, false);
	}

	/** Removes the mapping associated with the given {@link ImagePlus}. */
	public void unregisterLegacyImage(final ImagePlus imp) {
		unregisterLegacyImage(imp, true);
	}

	/**
	 * As {@link #unregisterDisplay(ImageDisplay)}, with an optional toggle to
	 * delete the associated {@link ImagePlus}.
	 */
	public void unregisterDisplay(final ImageDisplay display, final boolean deleteImp) {
		final ImagePlus imp = lookupImagePlus(display);
		removeMapping(display, imp, deleteImp);
	}

	/**
	 * As {@link #unregisterLegacyImage(ImagePlus)}, with an optional toggle to
	 * delete the given {@link ImagePlus}.
	 */
	public void unregisterLegacyImage(final ImagePlus imp, final boolean deleteImp) {
		final ImageDisplay display = lookupDisplay(imp);
		removeMapping(display, imp, deleteImp);
	}

	/**
	 * Gets a list of {@link ImageDisplay} instances known to this legacy service.
	 * 
	 * @return a collection of {@link ImageDisplay} instances linked to legacy
	 *         {@link ImagePlus} instances.
	 */
	public Collection<ImageDisplay> getImageDisplays() {
		if (legacyService.isLegacyMode()) {
			return legacyDisplayTable.values();
		}
		return imagePlusTable.keySet();
	}

	/**
	 * Gets a list of {@link ImagePlus} instances known to this legacy service.
	 * 
	 * @return a collection of legacy {@link ImagePlus} instances linked to
	 *         {@link ImageDisplay} instances.
	 */
	public Collection<ImagePlus> getImagePlusInstances() {
		if (legacyService.isLegacyMode()) {
			return legacyDisplayTable.keySet();
		}
		return displayTable.keySet();
	}

	// -- Helper methods --

	/**
	 * Creates a mapping between a given {@link ImageDisplay} and
	 * {@link ImagePlus}.
	 */
	private void addMapping(final ImageDisplay display, final ImagePlus imp) {
		addMapping(display, imp, legacyService.isLegacyMode());
	}

	/**
	 * Creates a mapping between a given {@link ImageDisplay} and
	 * {@link ImagePlus}. If {@code createLegacyMappings} is true, the mappings
	 * will be added to the leagcy mode maps.
	 */
	private void addMapping(final ImageDisplay display, final ImagePlus imp,
		final boolean createLegacyMappings)
	{
		// System.out.println("CREATE MAPPING "+display+" to "+imp+
		// " isComposite()="+imp.isComposite());

		// Must remove old mappings to avoid memory leaks
		// Removal is tricky for the displayTable. Without removal different
		// ImagePluses and CompositeImages can point to the same ImageDisplay. To
		// avoid a memory leak and to stay consistent in our mappings we find
		// all current mappings and remove them before inserting new ones. This
		// ensures that a ImageDisplay is only linked with one ImagePlus or
		// CompositeImage.
		if (createLegacyMappings) {
			final ImageDisplay toRemove = legacyDisplayTable.remove(imp);
			if (toRemove != null) legacyImagePlusTable.remove(toRemove);
			legacyDisplayTable.put(imp, display);
			legacyImagePlusTable.put(display, new WeakReference<>(imp));
		}
		else {
			final ImagePlus toRemove = imagePlusTable.remove(display);
			if (toRemove != null) displayTable.remove(toRemove);
			imagePlusTable.put(display, imp);
			displayTable.put(imp, display);
		}

		clearImagePlusKey(display);
	}

	/**
	 * Removes the {@link #IMP_KEY} entry from the {@link Dataset} attached to
	 * the given {@link ImageDisplay} - if any.
	 */
	private void clearImagePlusKey(final ImageDisplay display) {
		Data data = display.getActiveView().getData();
		if (Dataset.class.isAssignableFrom(data.getClass())) {
			((Dataset)data).getProperties().remove(IMP_KEY);
		}
	}

	/**
	 * Removes the mappings created by {@link #addMapping(ImageDisplay, ImagePlus)}.
	 */
	private void removeMapping(final ImageDisplay display, final ImagePlus imp,
		final boolean deleteImp)
	{
		// System.out.println("REMOVE MAPPING "+display+" to "+imp+
		// " isComposite()="+imp.isComposite());

		if (display != null) {
			imagePlusTable.remove(display);
			legacyImagePlusTable.remove(display);
		}
		if (imp != null) {
			displayTable.remove(imp);
			legacyDisplayTable.remove(imp);
			imagePluses.remove(imp);
			if (deleteImp) LegacyUtils.deleteImagePlus(imp);
			else {
				final ImagePlus currImagePlus = WindowManager.getCurrentImage();
				if (imp == currImagePlus) WindowManager.setTempCurrentImage(null);
			}
		}
	}

	// -- Event handlers --

	/*
	Removing this code to fix bug #835. Rely on LegacyCommand to create
	ImagePluses as they are needed.

	@EventHandler
	protected void onEvent(final DisplayCreatedEvent event) {
		if (event.getObject() instanceof ImageDisplay) {
			registerDisplay((ImageDisplay) event.getObject());
		}
	}
	*/

	/** @param event */
	@EventHandler
	private void onEvent(final DisplayDeletedEvent event) {

		/* OLD COMMENT : no longer relevant except for testing purposes
		// Need to make sure:
		// - modern IJ Windows always close when legacy IJ close expected
		// Stack to Images, Split Channels, etc.
		// - No ImagePlus/Display mapping becomes a zombie in the
		// LegacyImageMap failing to get garbage collected.
		// - That modern IJ does not think legacy IJ initiated the ij1.close()
		 */
		if (event.getObject() instanceof ImageDisplay) {
			unregisterDisplay((ImageDisplay) event.getObject());
		}
	}

	/**
	 * Check if updated display is an {@link ImageDisplay} with a mapped
	 * {@link ImagePlus}. If so, call {@link ImagePlus#updateAndDraw()}.
	 */
	@EventHandler
	private void onEvent(final DisplayUpdatedEvent event) {
		final Display<?> display = event.getDisplay();
		if (display instanceof ImageDisplay) {
			final ImagePlus mappedImagePlus = lookupImagePlus((ImageDisplay) event.getDisplay());
			if (mappedImagePlus != null) mappedImagePlus.updateAndDraw();
		}
	}
}
