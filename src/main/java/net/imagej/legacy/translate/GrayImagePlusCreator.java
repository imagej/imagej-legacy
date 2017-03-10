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

	private final GrayPixelHarmonizer pixelHarmonizer;
	private final ColorTableHarmonizer colorTableHarmonizer;
	private final MetadataHarmonizer metadataHarmonizer;
	private final PlaneHarmonizer planeHarmonizer;
	private final PositionHarmonizer positionHarmonizer;
	private final NameHarmonizer nameHarmonizer;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private LogService log;

	// -- public interface --

	public GrayImagePlusCreator(final Context context) {
		setContext(context);
		pixelHarmonizer = new GrayPixelHarmonizer();
		colorTableHarmonizer = new ColorTableHarmonizer(imageDisplayService);
		metadataHarmonizer = new MetadataHarmonizer();
		planeHarmonizer = new PlaneHarmonizer(log);
		positionHarmonizer = new PositionHarmonizer();
		nameHarmonizer = new NameHarmonizer();
	}

	@Override
	public ImagePlus createLegacyImage(final ImageDisplay display) {
		final Dataset dataset = imageDisplayService.getActiveDataset(display);
		return createLegacyImage(dataset, display);
	}

	@Override
	public ImagePlus createLegacyImage(final Dataset ds) {
		return createLegacyImage(ds, null);
	}

	@Override
	public ImagePlus createLegacyImage(final Dataset dataset,
		final ImageDisplay display)
	{
		if (dataset == null) return null;
		final Img<?> img = dataset.getImgPlus().getImg();
		ImagePlus imp;
		if (AbstractCellImg.class.isAssignableFrom(img.getClass())) {
			imp = makeImagePlus(dataset, createVirtualStack(dataset));
		}
		else if (LegacyUtils.datasetIsIJ1Compatible(dataset)) {
			imp = makeExactImagePlus(dataset);
			planeHarmonizer.updateLegacyImage(dataset, imp);
		}
		else {
			imp = makeNearestTypeGrayImagePlus(dataset);
			pixelHarmonizer.updateLegacyImage(dataset, imp);
		}
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

	/**
	 * Makes an {@link ImagePlus} that matches dimensions of a {@link Dataset}.
	 * The data values of the ImagePlus to be populated later elsewhere.
	 * 
	 * @param ds - input Dataset to be shape compatible with
	 * @param planeMaker - a PlaneMaker to use to make type correct image planes
	 * @param makeDummyPlanes - save memory by allocating the minimum number of
	 *          planes for the case that we'll be reassigning the planes
	 *          immediately.
	 * @return an ImagePlus whose dimensions are IJ1 compatible with the input
	 *         Dataset.
	 */
	private ImagePlus makeImagePlus(final Dataset ds,
		final PlaneMaker planeMaker, final boolean makeDummyPlanes)
	{

		final int[] dimIndices = new int[5];
		final int[] dimValues = new int[5];
		LegacyUtils.getImagePlusDims(ds, dimIndices, dimValues);

		final int cCount = dimValues[2];
		final int zCount = dimValues[3];
		final int tCount = dimValues[4];

		final ImageStack stack = new ImageStack(dimValues[0], dimValues[1]);

		final Object dummyPlane =
			makeDummyPlanes ? planeMaker.makePlane(dimValues[0], dimValues[1]) : null;

		for (long t = 0; t < tCount; t++) {
			for (long z = 0; z < zCount; z++) {
				for (long c = 0; c < cCount; c++) {
					Object plane;
					if (makeDummyPlanes) {
						plane = dummyPlane;
					}
					else {
						plane = planeMaker.makePlane(dimValues[0], dimValues[1]);
					}
					stack.addSlice(null, plane);
				}
			}
		}

		final ImagePlus imp = makeImagePlus(ds, cCount, zCount, tCount, stack);
		if (ds.getType() instanceof ShortType) markAsSigned16Bit(imp);

		return imp;
	}

	/**
	 * Makes an {@link ImagePlus} from a {@link Dataset}. Data is exactly the same
	 * between them as planes of data are shared by reference. Assumes the Dataset
	 * can be represented via plane references (thus XYCZT and backed by
	 * {@link PlanarAccess} and in a type compatible with legacy ImageJ). Does not
	 * set the metadata of the ImagePlus. Throws an exception if Dataset axis 0 is
	 * not X or Dataset axis 1 is not Y.
	 */
	// TODO - check that Dataset can be represented exactly
	private ImagePlus makeExactImagePlus(final Dataset ds) {
		final int[] dimIndices = new int[5];
		final int[] dimValues = new int[5];
		LegacyUtils.getImagePlusDims(ds, dimIndices, dimValues);
		LegacyUtils.assertXYPlanesCorrectlyOriented(dimIndices);
		return makeImagePlus(ds, getPlaneMaker(ds), true);
	}

	/**
	 * Makes an {@link ImagePlus} from a {@link Dataset} whose dimensions match.
	 * The type of the ImagePlus is a legacy ImageJ type that can best represent
	 * the data with the least loss of data. Sometimes the legacy and modern types
	 * are the same type and sometimes they are not. The data values and metadata
	 * are not assigned. Assumes it will never be sent a color Dataset.
	 */
	private ImagePlus makeNearestTypeGrayImagePlus(final Dataset ds) {
		return makeImagePlus(ds, getPlaneMaker(ds), false);
	}

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

	/**
	 * Updates an {@link ImagePlus} so that legacy ImageJ treats it as a signed 16
	 * bit image
	 */
	private void markAsSigned16Bit(final ImagePlus imp) {
		final Calibration cal = imp.getCalibration();
		cal.setSigned16BitCalibration();
	}

	/**
	 * Finds the best {@link PlaneMaker} for a given {@link Dataset}. The best
	 * PlaneMaker is the one that makes planes in the type that can best represent
	 * the Dataset's data values in legacy ImageJ.
	 */
	private PlaneMaker getPlaneMaker(final Dataset ds) {
		final boolean signed = ds.isSigned();
		final boolean integer = ds.isInteger();
		final int bitsPerPixel = ds.getType().getBitsPerPixel();
		if (bitsPerPixel <= 8) {
			if (!signed && integer) return new BytePlaneMaker();
		}
		else if (bitsPerPixel <= 16) {
			if (integer) return new ShortPlaneMaker();
		}
		return new FloatPlaneMaker();
	}

	/** Helper class to simplify the making of planes of different type data. */
	private interface PlaneMaker {

		Object makePlane(int w, int h);
	}

	/** Makes planes of bytes given width & height. */
	private class BytePlaneMaker implements PlaneMaker {

		public BytePlaneMaker() {
			// nothing to do
		}

		@Override
		public Object makePlane(final int w, final int h) {
			return new byte[w * h];
		}
	}

	/** Makes planes of shorts given width & height. */
	private class ShortPlaneMaker implements PlaneMaker {

		public ShortPlaneMaker() {
			// nothing to do
		}

		@Override
		public Object makePlane(final int w, final int h) {
			return new short[w * h];
		}
	}

	/** Makes planes of floats given width & height. */
	private class FloatPlaneMaker implements PlaneMaker {

		public FloatPlaneMaker() {
			// nothing to do
		}

		@Override
		public Object makePlane(final int w, final int h) {
			return new float[w * h];
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ImageStack createVirtualStack(final Dataset ds) {
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
