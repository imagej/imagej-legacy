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

package net.imagej.legacy.translate;

import ij.ImagePlus;
import ij.VirtualStack;
import ij.process.ImageProcessor;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;

import org.scijava.Context;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

/**
 * Creates {@link ImageDisplay}s containing gray data values from
 * {@link ImagePlus}es.
 * 
 * @author Barry DeZonia
 */
public class GrayDisplayCreator extends AbstractDisplayCreator
{

	// -- instance variables --

	private final GrayPixelHarmonizer pixelHarmonizer;
	private final ColorTableHarmonizer colorTableHarmonizer;
	private final MetadataHarmonizer metadataHarmonizer;
	private final CompositeHarmonizer compositeHarmonizer;
	private final PlaneHarmonizer planeHarmonizer;
	private final OverlayHarmonizer overlayHarmonizer;
	private final PositionHarmonizer positionHarmonizer;
	private final NameHarmonizer nameHarmonizer;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private DatasetService datasetService;

	@Parameter
	private LogService log;

	// NB - OverlayHarmonizer required because IJ1 plugins can hatch displays
	// while avoiding the Harmonizer. Not required in the Display->ImagePlus
	// direction as Harmonizer always catches that case.

	// -- constructor --

	public GrayDisplayCreator(final Context context) {
		setContext(context);
		pixelHarmonizer = new GrayPixelHarmonizer();
		colorTableHarmonizer =
			new ColorTableHarmonizer(imageDisplayService);
		metadataHarmonizer = new MetadataHarmonizer();
		compositeHarmonizer = new CompositeHarmonizer();
		planeHarmonizer = new PlaneHarmonizer(log);
		overlayHarmonizer = new OverlayHarmonizer(context);
		positionHarmonizer = new PositionHarmonizer();
		nameHarmonizer = new NameHarmonizer();
	}

	// -- AbstractDisplayCreator methods --

	@Override
	protected Dataset makeDataset(ImagePlus imp, AxisType[] preferredOrder) {
		Dataset ds;
		if (imp.getType() == ImagePlus.COLOR_RGB) {
			ds = makeGrayDatasetFromColorImp(imp, preferredOrder);
		}
		else if (preferredOrder[0] == Axes.X && preferredOrder[1] == Axes.Y &&
			!imp.getCalibration().isSigned16Bit() &&
			!(imp.getStack() instanceof VirtualStack))
		{
			ds = makeExactDataset(imp, preferredOrder);
			planeHarmonizer.updateDataset(ds, imp);
		}
		else {
			ds = makeGrayDatasetFromGrayImp(imp, preferredOrder);
			pixelHarmonizer.updateDataset(ds, imp);
		}
		return ds;
	}

	@Override
	protected ImageDisplay makeDisplay(ImagePlus imp, AxisType[] preferredOrder) {
		if (imp.getType() == ImagePlus.COLOR_RGB) {
			return colorCase(imp, preferredOrder);
		}
		return grayCase(imp, preferredOrder);
	}

	// -- private interface --

	private ImageDisplay colorCase(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		final Dataset ds = getDataset(imp, preferredOrder);
		setDatasetGrayDataFromColorImp(ds, imp);
		metadataHarmonizer.updateDataset(ds, imp);
		compositeHarmonizer.updateDataset(ds, imp);

		// CTR FIXME
		final ImageDisplay display =
			(ImageDisplay) displayService.createDisplay(ds.getName(), ds);

		colorTableHarmonizer.updateDisplay(display, imp);
		// NB - correct thresholding behavior requires overlay harmonization after
		// color table harmonization
		overlayHarmonizer.updateDisplay(display, imp);
		positionHarmonizer.updateDisplay(display, imp);
		nameHarmonizer.updateDisplay(display, imp);

		return display;
	}

	private ImageDisplay grayCase(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		Dataset ds = getDataset(imp, preferredOrder);
		metadataHarmonizer.updateDataset(ds, imp);
		compositeHarmonizer.updateDataset(ds, imp);

		// CTR FIXME
		final ImageDisplay display =
			(ImageDisplay) displayService.createDisplay(ds.getName(), ds);

		colorTableHarmonizer.updateDisplay(display, imp);
		// NB - correct thresholding behavior requires overlay harmonization after
		// color table harmonization
		overlayHarmonizer.updateDisplay(display, imp);
		positionHarmonizer.updateDisplay(display, imp);
		nameHarmonizer.updateDisplay(display, imp);

		return display;
	}

	/**
	 * Makes a gray {@link Dataset} from a Color {@link ImagePlus} whose channel
	 * count > 1. The Dataset will have isRgbMerged() false, 3 times as many
	 * channels as the input ImagePlus, and bitsperPixel == 8. Does not populate
	 * the data of the returned Dataset. That is left to other utility methods.
	 * Does not set metadata of Dataset. Throws exceptions if input ImagePlus is
	 * not RGB.
	 */
	private Dataset makeGrayDatasetFromColorImp(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		final int x = imp.getWidth();
		final int y = imp.getHeight();
		final int c = imp.getNChannels();
		final int z = imp.getNSlices();
		final int t = imp.getNFrames();

		if (imp.getType() != ImagePlus.COLOR_RGB) {
			throw new IllegalArgumentException("this method designed for "
				+ "creating a gray Dataset from a multichannel RGB ImagePlus");
		}

		final int[] inputDims = new int[] { x, y, c * 3, z, t };
		final AxisType[] axes = LegacyUtils.orderedAxes(preferredOrder, inputDims);
		final long[] dims = LegacyUtils.orderedDims(axes, inputDims);
		final String name = imp.getTitle();
		final int bitsPerPixel = 8;
		final boolean signed = false;
		final boolean floating = false;
		final boolean virtual = imp.getStack().isVirtual();
		final Dataset ds =
			datasetService.create(dims, name, axes, bitsPerPixel, signed, floating,
				virtual);

		DatasetUtils.initColorTables(ds);

		return ds;
	}

	/**
	 * Assigns the data values of a gray {@link Dataset} from a paired
	 * multichannel color {@link ImagePlus}. Assumes the Dataset and ImagePlus
	 * have compatible dimensions. Gets values via {@link ImageProcessor}::get().
	 * Does not change the Dataset's metadata.
	 */
	private void setDatasetGrayDataFromColorImp(final Dataset ds,
		final ImagePlus imp)
	{
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int cIndex = ds.dimensionIndex(Axes.CHANNEL);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final int tIndex = ds.dimensionIndex(Axes.TIME);
		final int x = imp.getWidth();
		final int y = imp.getHeight();
		final int c = imp.getNChannels();
		final int z = imp.getNSlices();
		final int t = imp.getNFrames();
		int imagejPlaneNumber = 1;
		final RandomAccess<? extends RealType<?>> accessor =
			ds.getImgPlus().randomAccess();
		for (int ti = 0; ti < t; ti++) {
			if (tIndex >= 0) accessor.setPosition(ti, tIndex);
			for (int zi = 0; zi < z; zi++) {
				if (zIndex >= 0) accessor.setPosition(zi, zIndex);
				for (int ci = 0; ci < c; ci++) {
					final ImageProcessor proc =
						imp.getStack().getProcessor(imagejPlaneNumber++);
					for (int yi = 0; yi < y; yi++) {
						accessor.setPosition(yi, yIndex);
						for (int xi = 0; xi < x; xi++) {
							accessor.setPosition(xi, xIndex);
							final int value = proc.get(xi, yi);
							final int rValue = (value >> 16) & 0xff;
							final int gValue = (value >> 8) & 0xff;
							final int bValue = (value >> 0) & 0xff;
							accessor.setPosition(ci * 3 + 0, cIndex);
							accessor.get().setReal(rValue);
							accessor.setPosition(ci * 3 + 1, cIndex);
							accessor.get().setReal(gValue);
							accessor.setPosition(ci * 3 + 2, cIndex);
							accessor.get().setReal(bValue);
						}
					}
				}
			}
		}
		ds.update();
	}

	/**
	 * Makes a planar {@link Dataset} whose dimensions match a given
	 * {@link ImagePlus}. Assumes it will never be called with
	 * any kind of color ImagePlus. Does not set metadata of Dataset.
	 */
	private Dataset makeExactDataset(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		final int x = imp.getWidth();
		final int y = imp.getHeight();
		final int c = imp.getNChannels();
		final int z = imp.getNSlices();
		final int t = imp.getNFrames();
		final int[] inputDims = new int[] { x, y, c, z, t };
		final AxisType[] axes = LegacyUtils.orderedAxes(preferredOrder, inputDims);
		final long[] dims = LegacyUtils.orderedDims(axes, inputDims);
		final String name = imp.getTitle();
		final int bitsPerPixel = imp.getBitDepth();
		final boolean signed = isSigned(imp);
		final boolean floating = isFloating(imp);
		final boolean virtual = imp.getStack().isVirtual();
		final Dataset ds =
			datasetService.create(dims, name, axes, bitsPerPixel, signed, floating,
				virtual);

		DatasetUtils.initColorTables(ds);

		return ds;
	}

	/**
	 * Makes a gray {@link Dataset} from a gray {@link ImagePlus}. Assumes it will
	 * never be given a color RGB Imageplus. Does not populate the data of the
	 * returned Dataset. That is left to other utility methods. Does not set
	 * metadata of Dataset.
	 */
	private Dataset makeGrayDatasetFromGrayImp(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		final int x = imp.getWidth();
		final int y = imp.getHeight();
		final int c = imp.getNChannels();
		final int z = imp.getNSlices();
		final int t = imp.getNFrames();

		final int[] inputDims = new int[] { x, y, c, z, t };
		final AxisType[] axes = LegacyUtils.orderedAxes(preferredOrder, inputDims);
		final long[] dims = LegacyUtils.orderedDims(axes, inputDims);
		final String name = imp.getTitle();

		final int bitsPerPixel = imp.getBitDepth();
		final boolean signed = isSigned(imp);
		final boolean floating = isFloating(imp);

		final boolean virtual = imp.getStack().isVirtual();

		final Dataset ds =
			datasetService.create(dims, name, axes, bitsPerPixel, signed, floating,
				virtual);

		DatasetUtils.initColorTables(ds);

		return ds;
	}

	/** Returns true if an {@link ImagePlus} is of type GRAY32. */
	private boolean isGray32PixelType(final ImagePlus imp) {
		final int type = imp.getType();
		return type == ImagePlus.GRAY32;
	}

	/** Returns true if an {@link ImagePlus} is backed by a signed type. */
	private boolean isSigned(final ImagePlus imp) {
		if (imp.getCalibration().isSigned16Bit()) return true;
		return isGray32PixelType(imp);
	}

	/** Returns true if an {@link ImagePlus} is backed by a floating type. */
	private boolean isFloating(final ImagePlus imp) {
		return isGray32PixelType(imp);
	}

}
