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

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Roi;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.convert.TableListWrapper;
import net.imagej.legacy.translate.Harmonizer;
import net.imagej.legacy.translate.ImageTranslator;
import net.imagej.legacy.translate.LegacyUtils;
import net.imagej.overlay.Overlay;
import net.imagej.patcher.LegacyInjector;
import net.imagej.roi.ROIService;
import net.imagej.roi.ROITree;
import net.imagej.table.TableService;
import net.imagej.ui.viewer.image.ImageDisplayViewer;

import org.scijava.AbstractContextual;
import org.scijava.convert.ConvertService;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.display.event.DisplayDeletedEvent;
import org.scijava.display.event.DisplayUpdatedEvent;
import org.scijava.event.EventHandler;
import org.scijava.plugin.Parameter;
import org.scijava.table.Table;
import org.scijava.ui.viewer.DisplayWindow;

/**
 * An image map between ImageJ {@link ImagePlus} objects and ImageJ2
 * {@link ImageDisplay}s. Because every {@link ImagePlus} has a
 * corresponding {@link ImageWindow} and vice versa, it works out best to
 * associate each {@link ImagePlus} with an {@link ImageDisplay} rather than
 * with a {@link Dataset}.
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
	private final ImageTranslator imageTranslator;

	/**
	 * The legacy service corresponding to this image map.
	 */
	private final LegacyService legacyService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private ConvertService convertService;

	// -- Constructor --

	public LegacyImageMap(final LegacyService legacyService) {
		setContext(legacyService.getContext());
		this.legacyService = legacyService;
		imagePlusTable = new ConcurrentHashMap<>();
		displayTable = new ConcurrentHashMap<>();
		imageTranslator = new ImageTranslator(legacyService);
	}

	// -- LegacyImageMap methods --

	/**
	 * Gets the {@link ImageDisplay} corresponding to the given {@link ImagePlus},
	 * or null if there is no existing table entry.
	 */
	public ImageDisplay lookupDisplay(final ImagePlus imp) {
		if (imp == null) return null;
		ImageDisplay display;
		if (legacyService.isLegacyMode()) display = legacyDisplayTable.get(imp);
		else display = displayTable.get(imp);
		synchronizeAttachmentsToDataset(display, imp);
		return display;
	}

	/**
	 * Gets the {@link ImagePlus} corresponding to the given {@link ImageDisplay},
	 * or null if there is no existing table entry.
	 */
	public ImagePlus lookupImagePlus(final ImageDisplay display) {
		if (display == null) return null;
		ImagePlus imagePlus;
		if (legacyService.isLegacyMode()) {
			final WeakReference<ImagePlus> weakReference = legacyImagePlusTable.get(
				display);
			imagePlus = weakReference == null ? null : weakReference.get();
		}
		else imagePlus = imagePlusTable.get(display);
		synchronizeAttachmentsToImagePlus(imagePlus, display);
		return imagePlus;
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
		final ImageDisplay display = (ImageDisplay) displayService.createDisplay(ds
			.getName(), ds);
		addMapping(display, imp);
		synchronizeAttachmentsToImagePlus(imp, ds);
		return imp;
	}

	/**
	 * Ensures that the given {@link ImageDisplay} has a corresponding
	 * {@link ImagePlus}.
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
		synchronizeAttachmentsToImagePlus(imp, display);
		return imp;
	}

	/**
	 * Ensures that the given {@link ImagePlus} has a corresponding
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
			// mapping does not exist; mirror ImagePlus to ImageDisplay
			display = imageTranslator.createDisplay(imp);
			addMapping(display, imp);
		}
		synchronizeAttachmentsToDataset(display, imp);
		return display;
	}

	public synchronized void toggleLegacyMode(boolean enteringLegacyMode) {
		if (enteringLegacyMode)
			enterLegacyMode();
		else
			leaveLegacyMode();
	}

	private void enterLegacyMode()
	{
		final Harmonizer harmonizer = new Harmonizer(legacyService.getContext(), imageTranslator);
		// migrate from the ImagePlusTable and DisplayTable to legacy versions.
		final List<ImageDisplay> imageDisplays =
				imageDisplayService.getImageDisplays();
		// TODO: this is almost exactly what LegacyCommand does, so it is
		// pretty obvious that it is misplaced in there.
		for (final ImageDisplay display : imageDisplays) {
			ImagePlus imp = lookupImagePlus(display);
			if (imp == null) {
				final Dataset ds = imageDisplayService.getActiveDataset(display);
				if ( LegacyUtils.dimensionsIJ1Compatible(ds)) {
					// Ensure the mappings are registered in the legacy maps
					imp = registerDisplay(display, true);
					final ImageDisplayViewer viewer =
							(ImageDisplayViewer) legacyService.uiService().getDisplayViewer(display);
					if (viewer != null) {
						final DisplayWindow window = viewer.getWindow();
						if (window != null) window.showDisplay(false);
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

	private void leaveLegacyMode()
	{
		final Harmonizer harmonizer = new Harmonizer(legacyService.getContext(), imageTranslator);
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
		if (deleteImp && imp != null) imp.close();
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
	 * @return a collection of {@link ImageDisplay} instances linked to
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
	 * @return a collection of {@link ImagePlus} instances linked to
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
	 * will be added to the legacy mode maps.
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

	/**
	 * Ensures that the given {@link ImagePlus} has the same ROIs/tables attached
	 * as those attached to the active {@link Dataset} in the given
	 * {@link ImageDisplay}.
	 * <p>
	 * All ROIs/tables associated with the active {@link Dataset}, are converted
	 * and attached to the given {@link ImagePlus}. Such that both the active
	 * {@link Dataset} and {@link ImagePlus} have equivalent ROIs and tables.
	 * </p>
	 *
	 * @param imagePlus The {@link ImagePlus} to attach ROIs/tables to
	 * @param display The {@link ImageDisplay} whose active {@link Dataset}'s
	 *          ROIs/tables will be converted and referenced by the ImagePlus
	 */
	private void synchronizeAttachmentsToImagePlus(final ImagePlus imagePlus,
		final ImageDisplay display)
	{
		synchronizeAttachmentsToImagePlus(imagePlus, imageDisplayService
			.getActiveDataset(display));
	}

	/**
	 * Ensures that the given {@link ImagePlus} has the same ROIs/tables attached
	 * as those attached to the given {@link Dataset}.
	 * <p>
	 * All ROIs/tables associated with the given {@link Dataset}, are converted
	 * and attached to the given {@link ImagePlus}. Such that both the
	 * {@link Dataset} and {@link ImagePlus} have equivalent ROIs and tables.
	 * </p>
	 *
	 * @param imagePlus The {@link ImagePlus} to attach ROIs/tables to
	 * @param dataset The {@link Dataset} whose ROIs/tables will be converted and
	 *          referenced by the ImagePlus
	 */
	private void synchronizeAttachmentsToImagePlus(final ImagePlus imagePlus,
		final Dataset dataset)
	{
		if (dataset == null || imagePlus == null) return;

		// ROIs
		if (dataset.getProperties().get(ROIService.ROI_PROPERTY) != null) {
			final ij.gui.Overlay o = convertService.convert(dataset.getProperties()
				.get(ROIService.ROI_PROPERTY), ij.gui.Overlay.class);
			if (o == null) return;
			imagePlus.setOverlay(o);
		}
		else {
			imagePlus.setOverlay(null);
			imagePlus.deleteRoi();
		}

		// Tables
		if (dataset.getProperties().get(TableService.TABLE_PROPERTY) != null &&
			dataset.getProperties().get(
				TableService.TABLE_PROPERTY) instanceof List)
		{
			@SuppressWarnings("unchecked")
			final List<Table<?, ?>> tables = (List<Table<?, ?>>) dataset
				.getProperties().get(TableService.TABLE_PROPERTY);
			final List<ij.measure.ResultsTable> ijTables = new TableListWrapper(
				tables, convertService);
			imagePlus.setProperty("tables", ijTables);
		}
		else {
			if (imagePlus.getProperty("tables") == null) return;
			imagePlus.getProperties().remove("tables");
		}
	}

	/**
	 * Ensures that the active {@link Dataset} of the given {@link ImageDisplay}
	 * has equivalent ROIs/tables attached as those associated with the given
	 * {@link ImagePlus}.
	 * <p>
	 * All ROIs/tables associated with the given {@link ImagePlus} are converted
	 * and attached to the active {@link Dataset}. Such that both the given
	 * {@link ImagePlus} and the active {@link Dataset} have equivalent ROIs and
	 * tables attached.
	 * </p>
	 *
	 * @param display The {@link ImageDisplay} whose active {@link Dataset} will
	 *          have equivalent ROIs/tables attached
	 * @param imagePlus The {@link ImagePlus} whose ROIs/tables will be converted
	 *          and referenced by the active Dataset
	 */
	private void synchronizeAttachmentsToDataset(final ImageDisplay display,
		final ImagePlus imagePlus)
	{
		synchronizeAttachmentsToDataset(imageDisplayService.getActiveDataset(
			display), imagePlus);
	}

	/**
	 * Ensures that the given {@link Dataset} has equivalent ROIs/tables attached
	 * as those associated with the given {@link ImagePlus}.
	 * <p>
	 * All ROIs/tables associated with the given {@link ImagePlus} are converted
	 * and attached to the {@link Dataset}. Such that both the given
	 * {@link ImagePlus} and the {@link Dataset} have equivalent ROIs and tables
	 * attached.
	 * </p>
	 *
	 * @param dataset The {@link Dataset} to which equivalent ROIs/tables will be
	 *          attached
	 * @param imagePlus The {@link ImagePlus} whose ROIs/tables will be converted
	 *          and referenced by the active Dataset
	 */
	private void synchronizeAttachmentsToDataset(final Dataset dataset,
		final ImagePlus imagePlus)
	{
		if (dataset == null || imagePlus == null) return;

		// ROIs
		if (imagePlus.getOverlay() != null && imagePlus.getOverlay().size() > 0) {
			if (imagePlus.getRoi() != null && !imagePlus.getOverlay().contains(
				imagePlus.getRoi())) imagePlus.getOverlay().add(imagePlus.getRoi());
			final ROITree rois = convertService.convert(imagePlus.getOverlay(),
				ROITree.class);
			dataset.getProperties().put(ROIService.ROI_PROPERTY, rois);
		}
		else if (imagePlus.getRoi() != null) {
			final ij.gui.Overlay o = new ij.gui.Overlay();
			o.add(imagePlus.getRoi());
			final ROITree rois = convertService.convert(o, ROITree.class);
			dataset.getProperties().put(ROIService.ROI_PROPERTY, rois);
		}
		else dataset.getProperties().remove(ROIService.ROI_PROPERTY);

		// Tables
		if (imagePlus.getProperty("tables") != null && imagePlus.getProperty(
			"tables") instanceof List)
		{
			final List<?> tables = (List<?>) imagePlus.getProperty("tables");
			List<Table<?, ?>> imagejTables;
			if (tables instanceof TableListWrapper) imagejTables =
				((TableListWrapper) tables).getUpdatedSource();
			else {
				imagejTables = new ArrayList<>();
				for (final Object table : tables) {
					if (!(table instanceof ij.measure.ResultsTable)) continue;
					final Table<?, ?> t = convertService.convert(table, Table.class);
					if (t != null) imagejTables.add(t);
				}
			}
			dataset.getProperties().put(TableService.TABLE_PROPERTY, imagejTables);
		}
		else dataset.getProperties().remove(TableService.TABLE_PROPERTY);
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

	/**
	 * Disposes the {@link ij.ImagePlus} (if any) tied to the deleted
	 * {@link ImageDisplay}.
	 *
	 * @param event Event reporting the {@link ImageDisplay} being deleted.
	 */
	@EventHandler
	private void onEvent(final DisplayDeletedEvent event) {
		if (event.getObject() instanceof ImageDisplay) {
			unregisterDisplay((ImageDisplay) event.getObject(), true);
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
			if (mappedImagePlus != null) {
				final Harmonizer harmonizer =
						new Harmonizer(legacyService.getContext(), imageTranslator);
				harmonizer.updateLegacyImage((ImageDisplay) display, mappedImagePlus);
				mappedImagePlus.updateAndDraw();
			}
		}
	}
}
