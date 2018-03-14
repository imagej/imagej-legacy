/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2018 Board of Regents of the University of
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

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;

import java.util.Arrays;
import java.util.List;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.display.ColorMode;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.PlanarAccess;
import net.imglib2.img.cell.AbstractCellImg;
import net.imglib2.img.display.imagej.ImageJVirtualStack;
import net.imglib2.img.display.imagej.ImageJVirtualStackFloat;
import net.imglib2.img.display.imagej.ImageJVirtualStackUnsignedByte;
import net.imglib2.img.display.imagej.ImageJVirtualStackUnsignedShort;
import net.imglib2.transform.integer.MixedTransform;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.MixedTransformView;
import net.imglib2.view.Views;

import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

/**
 * Creates {@link ImagePlus}es from {@link ImageDisplay}s containing gray data.
 * 
 * @author Barry DeZonia
 */
public class GrayImagePlusCreator extends AbstractImagePlusCreator {

	// -- instance variables --

	private final ColorTableHarmonizer colorTableHarmonizer;
	private final MetadataHarmonizer metadataHarmonizer;
	private final PositionHarmonizer positionHarmonizer;
	private final NameHarmonizer nameHarmonizer;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private LogService log;

	// -- public interface --

	public GrayImagePlusCreator(final Context context) {
		setContext(context);
		colorTableHarmonizer = new ColorTableHarmonizer(imageDisplayService);
		metadataHarmonizer = new MetadataHarmonizer();
		positionHarmonizer = new PositionHarmonizer();
		nameHarmonizer = new NameHarmonizer();
	}

	public ImagePlus createLegacyImage(final ImageDisplay display) {
		final Dataset dataset = imageDisplayService.getActiveDataset(display);
		return createLegacyImage(dataset, display);
	}

	public ImagePlus createLegacyImage(final Dataset ds) {
		return createLegacyImage(ds, null);
	}

	public ImagePlus createLegacyImage(final Dataset dataset,
		final ImageDisplay display)
	{
		if (dataset == null) return null;
		final Img<?> img = dataset.getImgPlus().getImg();
		ImagePlus imp;
			imp = makeImagePlus(dataset, createVirtualStack(dataset));
		metadataHarmonizer.updateLegacyImage(dataset, imp);

		populateCalibrationData(imp, dataset);

		if (display != null) {
			if (shouldBeComposite(display, dataset, imp)) {
				imp = makeCompositeImage(imp);
			}
			colorTableHarmonizer.updateLegacyImage(display, imp);
			positionHarmonizer.updateLegacyImage(display, imp);
			nameHarmonizer.updateLegacyImage(display, imp);
		}

		return imp;
	}

	// -- private interface --

	private boolean shouldBeComposite(final ImageDisplay display,
		final Dataset ds, final ImagePlus imp)
	{
		final int channels = imp.getNChannels();
		if (channels < 2 || channels > 7) return false;
		final DatasetView view = imageDisplayService.getActiveDatasetView(display);
		if (view != null && view.getColorMode() == ColorMode.COMPOSITE) return true;
		if (ds.getCompositeChannelCount() == 1) return false;
		return true;
	}

	/**
	 * Makes a {@link CompositeImage} that wraps a given {@link ImagePlus} and
	 * sets channel LUTs to match how modern ImageJ displays the given paired
	 * {@link Dataset}. Assumes given ImagePlus has channels in range 2..7 and
	 * that if Dataset View has ColorTables defined there is one per channel.
	 */
	// TODO - last assumption may be bad. If I have a 6 channel Dataset with
	// compos chann count == 2 then maybe I only have 2 or 3 ColorTables. Is
	// this configuration even valid. If so then what to do for translation?
	private CompositeImage makeCompositeImage(final ImagePlus imp) {
		return new CompositeImage(imp, CompositeImage.COMPOSITE);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	ImageStack createVirtualStack(final Dataset ds) {
		return createVirtualStack((ImgPlus) ds.getImgPlus(), ds.isSigned());
	}

	private <T extends RealType<T>> ImageStack createVirtualStack(
		final ImgPlus<T> imgPlus, final boolean isSigned)
	{
		final RealType<T> type = imgPlus.firstElement();
		final int bitDepth = type.getBitsPerPixel();
		// TODO : what about ARGB type's CellImgs? Note also that ARGB is not a
		// RealType and thus our dataset can't support it directly.

		// ensure image being passed to imglib2-ij has XYCZT dimension order
		RandomAccessibleInterval<T> rai = ensureXYCZT(imgPlus);

		ImageJVirtualStack stack = null;

		// finally, wrap the XYCZT image as an ImageJ virtual stack
		if (bitDepth <= 8 && !isSigned) {
			stack = new ImageJVirtualStackUnsignedByte<>(rai, new ByteConverter<T>());
		}
		else if (bitDepth <= 16 && !isSigned) {
			stack = new ImageJVirtualStackUnsignedShort<>(rai,
				new ShortConverter<T>());
		}
		else { // other types translated as 32-bit float data
			stack = new ImageJVirtualStackFloat<>(rai, new FloatConverter<T>());
		}

		// Virtual stacks are writable when backed by a CellCache!
		stack.setWritable(true);

		return stack;
	}

	private static final List<AxisType> naturalOrder =
			Arrays.asList(Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME);

	protected <T> RandomAccessibleInterval<T> ensureXYCZT(final ImgPlus<T> imgPlus) {
		boolean inNaturalOrder = true;
		final int[] axes = new int[5];
		final boolean[] matchedDimensions = new boolean[5];
		final long[] min = new long[5], max = new long[5];
		for (int d = 0; d < imgPlus.numDimensions(); d++) {
			final CalibratedAxis axis = imgPlus.axis(d);
			final int index = naturalOrder.indexOf(axis.type());
			if (index < 0) {
				// Axis isn't present. maybe warn instead?
				throw new IllegalArgumentException("Unsupported axis type: " + axis.type());
			}
			axes[d] = index;
			matchedDimensions[index] = true;
			min[index] = imgPlus.min(d);
			max[index] = imgPlus.max(d);
			if (index != d) inNaturalOrder = false;
		}

		if (imgPlus.numDimensions() != 5) inNaturalOrder = false;
		if (inNaturalOrder) return imgPlus;

		RandomAccessibleInterval<T> rai = imgPlus;
		// pad the image to at least 5D
		for (int i = 0; i < 5; i++) {
			if (matchedDimensions[i]) continue;
			axes[rai.numDimensions()] = i;
			min[i] = 0;
			max[i] = 0;
			rai = Views.addDimension(rai, 0, 0);
		}

		// permute the axis order to XYCZT...
		final MixedTransform t = new MixedTransform(rai.numDimensions(), 5);
		t.setComponentMapping(axes);
		return Views.interval(new MixedTransformView< >( rai, t ), min, max);
	}

	private class ByteConverter<S extends RealType<S>> implements
		Converter<S, UnsignedByteType>
	{

		@Override
		public void convert(final S input, final UnsignedByteType output) {
			double val = input.getRealDouble();
			if (val < 0) val = 0;
			else if (val > 255) val = 255;
			output.setReal(val);
		}

	}

	private class ShortConverter<S extends RealType<S>> implements
		Converter<S, UnsignedShortType>
	{

		@Override
		public void convert(final S input, final UnsignedShortType output) {
			double val = input.getRealDouble();
			if (val < 0) val = 0;
			else if (val > 65535) val = 65535;
			output.setReal(val);
		}

	}

	private class FloatConverter<S extends RealType<S>> implements
		Converter<S, FloatType>
	{

		@Override
		public void convert(final S input, final FloatType output) {
			double val = input.getRealDouble();
			if (val < -Float.MAX_VALUE) val = -Float.MAX_VALUE;
			else if (val > Float.MAX_VALUE) val = Float.MAX_VALUE;
			output.setReal(val);
		}

	}

}
