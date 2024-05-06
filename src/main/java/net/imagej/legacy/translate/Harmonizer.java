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

package net.imagej.legacy.translate;

import ij.ImagePlus;
import ij.ImageStack;

import java.util.HashMap;
import java.util.Map;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.LegacyService;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

/**
 * Provides methods for synchronizing data between an {@link ImageDisplay} and
 * an {@link ImagePlus}.
 * 
 * @author Barry DeZonia
 */
public class Harmonizer extends AbstractContextual {

	// -- instance variables --

	private final ImageTranslator imageTranslator;
	private final Map<ImagePlus, Integer> bitDepthMap;

	private final ColorTableHarmonizer colorTableHarmonizer;
	private final MetadataHarmonizer metadataHarmonizer;
	private final CompositeHarmonizer compositeHarmonizer;
	private final OverlayHarmonizer overlayHarmonizer;
	private final PositionHarmonizer positionHarmonizer;
	private final NameHarmonizer nameHarmonizer;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private LegacyService legacyService;

	// -- constructor --

	public Harmonizer(final Context context, final ImageTranslator trans)
	{
		setContext(context);
		imageTranslator = trans;
		bitDepthMap = new HashMap<>();
		colorTableHarmonizer = new ColorTableHarmonizer(imageDisplayService);
		metadataHarmonizer = new MetadataHarmonizer();
		compositeHarmonizer = new CompositeHarmonizer();
		overlayHarmonizer = new OverlayHarmonizer(context);
		positionHarmonizer = new PositionHarmonizer();
		nameHarmonizer = new NameHarmonizer();
	}

	// -- public interface --

	/**
	 * Changes the data within an {@link ImagePlus} to match data in a
	 * {@link ImageDisplay}. Assumes Dataset has planar primitive access in a
	 * legacy ImageJ compatible format.
	 */
	public void
		updateLegacyImage(final ImageDisplay display, final ImagePlus imp)
	{
		final Dataset ds = imageDisplayService.getActiveDataset(display);
		rebuildImagePlusData(display, imp);
		metadataHarmonizer.updateLegacyImage(ds, imp);
		colorTableHarmonizer.updateLegacyImage(display, imp);
		// NB - correct thresholding behavior requires overlay harmonization after
		// color table harmonization
		overlayHarmonizer.updateLegacyImage(display, imp);
		positionHarmonizer.updateLegacyImage(display, imp);
		nameHarmonizer.updateLegacyImage(display, imp);
	}

	/**
	 * Changes the data within a {@link ImageDisplay} to match data in an
	 * {@link ImagePlus}. Assumes the given ImagePlus is not a degenerate set of
	 * data (an empty stack).
	 */
	public void updateDisplay(final ImageDisplay display, final ImagePlus imp) {

		// NB - if ImagePlus is degenerate the following code can fail. This is
		// because imglib cannot represent an empty data container. So we catch
		// the issue here:

		if (imp.getStack().getSize() == 0)
			throw new IllegalArgumentException(
					"cannot update a display with an ImagePlus that has an empty stack");

		final Dataset ds = imageDisplayService.getActiveDataset(display);

		// did type of ImagePlus change?
		Integer oldBitDepth = bitDepthMap.get(imp);

		// NB
		// if old bit depth is null then plugin created a new display. although
		// nearly every time the data is already correct there are places in IJ1
		// (such as the Histogram plugin) where the data in the created display
		// has not been updated to reflect values in imp. So record the bit depth
		// but don't return or pixels won't get synchronized correctly.
		if (oldBitDepth == null) {
			oldBitDepth = imp.getBitDepth();
			bitDepthMap.put(imp, imp.getBitDepth());
		}
		rebuildDatasetData(ds, imp);
		metadataHarmonizer.updateDataset(ds, imp);
		compositeHarmonizer.updateDataset(ds, imp);
		colorTableHarmonizer.updateDisplay(display, imp);
		// NB - correct thresholding behavior requires overlay harmonization after
		// color table harmonization
		overlayHarmonizer.updateDisplay(display, imp);
		positionHarmonizer.updateDisplay(display, imp);
		nameHarmonizer.updateDisplay(display, imp);

		// TODO - this should not be necessary but Blobs will not display inverted
		// without this. When we change the update mechanism so that drawing only
		// happens in the display code after it has collected all info about updates
		// we should remove this. See bug #915
		// Note BDZ 8-28-12. I've commented out the fix on next line. Note that it
		// no longer seems necessary (after testing). But let's leave the comment
		// here for a while in case it turns out to indeed be needed later. 
		//ds.update();
	}

	/**
	 * Remembers the type of an {@link ImagePlus}. This type can be checked after
	 * a call to a plugin to see if the ImagePlus underwent a type change.
	 */
	public void registerType(final ImagePlus imp) {
		if (imp == null) return;
		bitDepthMap.put(imp, imp.getBitDepth());
	}

	// -- private interface --

	/**
	 * Creates a new {@link ImageStack} of data from a {@link ImageDisplay} and
	 * assigns it to given {@link ImagePlus}
	 * 
	 * @param display
	 * @param imp
	 */
	private void rebuildImagePlusData(final ImageDisplay display,
		final ImagePlus imp)
	{
		final ImagePlus newImp = legacyService.getImageMap().registerDisplay(display);
		if(imp == newImp)
			return;
		imp.setStack(newImp.getStack());
		final int c = newImp.getNChannels();
		final int z = newImp.getNSlices();
		final int t = newImp.getNFrames();
		imp.setDimensions(c, z, t);
		imp.setOpenAsHyperStack(imp.getNDimensions() > 3);
		LegacyUtils.deleteImagePlus(newImp);
	}

	/**
	 * Modifies a given {@link Dataset} to incorporate all new data from a legacy
	 * {@link ImagePlus}. Internally the Dataset refers to an all new {@link
	 * ImgPlus}. 
	 */
	private void rebuildDatasetData(final Dataset ds, final ImagePlus imp)
	{
		// NB - create display from copy of original ImagePlus? Not right now. But
		// will need to in future if createDisplay() registers "imp" with legacy
		// image map. If that were the case we'd have two displays referring to a
		// single ImagePlus which could be problematic. But since we're not
		// registering right now avoid the runtime penalty and memory overhead.
		//final ImagePlus impCopy = imp.duplicate();
		//final ImageDisplay tmpDisplay =
		//		imageTranslator.createDisplay(impCopy, ds.getAxes());
		final ImageDisplay tmpDisplay = imageTranslator.createDisplay(imp);
		final Dataset tmpDs = imageDisplayService.getActiveDataset(tmpDisplay);
		ds.setImgPlus(tmpDs.getImgPlus());
		ds.setRGBMerged(tmpDs.isRGBMerged());
		tmpDisplay.close();
	}
	
	// NOTE: to propagate a VirtualStack's first plane pixel changes we save it
	// early in the harmonization process and refer to it later. This code is part
	// of that process
}
