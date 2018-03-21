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
import ij.WindowManager;
import ij.macro.Interpreter;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.Axis;
import net.imagej.axis.AxisType;
import net.imglib2.img.basictypeaccess.PlanarAccess;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;

/**
 * A bag of static methods used throughout the translation layer
 * 
 * @author Barry DeZonia
 */
public class LegacyUtils {

	// -- static variables --

	private final static AxisType[] defaultAxes = new AxisType[] { Axes.X,
		Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME };

	// -- public static methods --

	// TODO - deleteImagePlus() could be better located in some other class.
	// It's use of an ImagePlus does not fit in with theme of other methods
	// in this class.

	/**
	 * Modifies legacy ImageJ data structures so that there are no dangling
	 * references to an obsolete ImagePlus.
	 */
	public static void deleteImagePlus(final ImagePlus imp) {
		final ImagePlus currImagePlus = WindowManager.getCurrentImage();
		if (imp == currImagePlus) WindowManager.setTempCurrentImage(null);
		Interpreter.removeBatchModeImage(imp);
	}

	/**
	 * Returns true if any of the given Axes cannot be represented in an legacy
	 * ImageJ ImagePlus.
	 */
	static boolean hasNonIJ1Axes(Dataset ds) {
		for (int i = 0; i < ds.numDimensions(); i++) {
			AxisType axisType = ds.axis(i).type();
			if (axisType == Axes.X) continue;
			if (axisType == Axes.Y) continue;
			if (axisType == Axes.CHANNEL) continue;
			if (axisType == Axes.Z) continue;
			if (axisType == Axes.TIME) continue;
			return true;
		}
		return false;
	}

	/**
	 * Returns the number of planes needed in legacy ImageJ to represent all
	 * the axes of a modern ImageJ Dataset. Incompatible modern axes are encoded
	 * as extra planes in the legacy ImageJ image.
	 */
	static long ij1PlaneCount(ImgPlus imgPlus, final AxisType whiteList) {
		long planeCount = 1;
		int axisIndex = 0;
		for ( int i = 0; i < imgPlus.numDimensions(); i++) {
			Axis axisType = imgPlus.axis(i);
			final long axisSize = imgPlus.dimension(axisIndex++);
			// we want to skip the X,Y,C,Z,T axes, unless that axis is the white list.
			// this will cause planeCount to be the product of the whiteList axis and
			// all other axes
			if (axisType == whiteList) {} // DON'T continue
			else if (axisType == Axes.X) continue;
			else if (axisType == Axes.Y) continue;
			else if (axisType == Axes.CHANNEL) continue;
			else if (axisType == Axes.Z) continue;
			else if (axisType == Axes.TIME) continue;
			planeCount *= axisSize;
		}
		return planeCount;
	}

	/**
	 * Determines if a Dataset's dimensions cannot be represented within a legacy
	 * ImageJ ImageStack. Returns true if the Dataset does not have X or Y axes.
	 * Returns true if the XY plane size is greater than Integer.MAX_VALUE.
	 * Returns true if the number of planes is greater than Integer.MAX_VALUE.
	 */
	public static boolean dimensionsIJ1Compatible(final Dataset ds) {
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final int cIndex = ds.dimensionIndex(Axes.CHANNEL);

		final long[] dims = Intervals.dimensionsAsLongArray(ds);

		final long xCount = xIndex < 0 ? 1 : dims[xIndex];
		final long yCount = yIndex < 0 ? 1 : dims[yIndex];
		final long zCount = zIndex < 0 ? 1 : dims[zIndex];
		final long tCount = ij1PlaneCount(ds.getImgPlus(), Axes.TIME);
		final long cCount = cIndex < 0 ? 1 : dims[cIndex];
		final long ij1ChannelCount = ds.isRGBMerged() ? (cCount / 3) : cCount;

		// check width exists
		if (xIndex < 0) return false;

		// check height exists
		if (yIndex < 0) return false;

		// check plane size not too large
		if ((xCount * yCount) > Integer.MAX_VALUE) return false;

		// check number of planes not too large
		if (ij1ChannelCount * zCount * tCount > Integer.MAX_VALUE) return false;

		return true;
	}

	// -- package access static methods --

	static AxisType[] getPreferredAxisOrder() {
		return defaultAxes;
	}

	/**
	 * tests that a given {@link Dataset} can be represented as a color
	 * {@link ImagePlus}. Some of this test maybe overkill if by definition
	 * rgbMerged can only be true if channels == 3 and type = ubyte are also true.
	 */
	static boolean isColorCompatible(final Dataset ds) {
		final int compositeChannels = ds.getImgPlus().getCompositeChannelCount();

		if (ds.isRGBMerged()) {
			if (!ds.isInteger()) return false;
			if (ds.isSigned()) return false;
			if (ds.getType().getBitsPerPixel() != 8) return false;
			if (compositeChannels < 1 || compositeChannels > 3) return false;
			return true;
		}

		if (compositeChannels >= 2 && compositeChannels <= 7) return true;

		return false;
	}

	/**
	 * Copies a {@link Dataset}'s dimensions and axis indices into provided
	 * arrays. The order of dimensions is formatted to be X,Y,C,Z,T. If an axis is
	 * not present in the Dataset its value is set to 1 and its index is set to
	 * -1. Combines all non XYZT axis dimensions into multiple C dimensions.
	 */
	static void getImagePlusDims(final Dataset dataset,
		final int[] outputIndices, final int[] outputDims)
	{
		// get axis indices
		final int xIndex = dataset.dimensionIndex(Axes.X);
		final int yIndex = dataset.dimensionIndex(Axes.Y);
		final int cIndex = dataset.dimensionIndex(Axes.CHANNEL);
		final int zIndex = dataset.dimensionIndex(Axes.Z);
		final int tIndex = dataset.dimensionIndex(Axes.TIME);

		final long xCount = xIndex < 0 ? 1 : dataset.dimension(xIndex);
		final long yCount = yIndex < 0 ? 1 : dataset.dimension(yIndex);
		final long zCount = zIndex < 0 ? 1 : dataset.dimension(zIndex);
		final long tCount = ij1PlaneCount(dataset.getImgPlus(), Axes.TIME);
		final long cCount = cIndex < 0 ? 1 : dataset.dimension(cIndex);

		// NB - cIndex tells what dimension is channel in Dataset. For a
		// Dataset that encodes other axes as channels this info is not so
		// useful. But for Datasets that can be represented exactly it is.
		// So pass along info but API consumers must be careful to not make
		// assumptions.

		// set output values : indices
		outputIndices[0] = xIndex;
		outputIndices[1] = yIndex;
		outputIndices[2] = cIndex;
		outputIndices[3] = zIndex;
		outputIndices[4] = tIndex;

		// set output values : dimensions
		outputDims[0] = (int) xCount;
		outputDims[1] = (int) yCount;
		outputDims[2] = (int) cCount;
		outputDims[3] = (int) zCount;
		outputDims[4] = (int) tCount;
	}

	/**
	 * Throws an Exception if the planes of a Dataset are not compatible with
	 * legacy ImageJ.
	 */
	static void assertXYPlanesCorrectlyOriented(final int[] dimIndices) {
		if (dimIndices[0] != 0) {
			throw new IllegalArgumentException(
				"Dataset does not have X as the first axis");
		}
		if (dimIndices[1] != 1) {
			throw new IllegalArgumentException(
				"Dataset does not have Y as the second axis");
		}
	}

	/**
	 * Returns true if a {@link Dataset} can be represented by reference in legacy
	 * ImageJ.
	 */
	static boolean datasetIsIJ1Compatible(final Dataset ds) {
		if (ds == null) return true;
		if (LegacyUtils.hasNonIJ1Axes(ds)) return false;
		return ij1StorageCompatible(ds) && ij1TypeCompatible(ds);
	}

	/**
	 * Fills legacy ImageJ incompatible indices of a position array. The channel
	 * from legacy ImageJ is rasterized into potentially multiple indices in the
	 * modern ImageJ image's position array. For instance a modern image with
	 * CHANNELs and SPECTRA gets encoded with multiple channels in legacy ImageJ.
	 * When coming back from legacy ImageJ need to rasterize the single legacy
	 * channel position back into (CHANNEL,SPECTRA) pairs.
	 * 
	 * @param dims - the dimensions of the modern ImageJ Dataset
	 * @param axes - the axes labels that match the Dataset dimensions
	 * @param ij1Channel - the channel value in legacy ImageJ (to be decoded for
	 *          modern ImageJ)
	 * @param pos - the position array to fill with rasterized values
	 */
	static void fillChannelIndices(final long[] dims, final AxisType[] axes,
		final long ij1Channel, final long[] pos)
	{
		long workingIndex = ij1Channel;
		for (int i = 0; i < dims.length; i++) {
			final AxisType axis = axes[i];
			// skip axes we don't encode as channels
			if (axis == Axes.X) continue;
			if (axis == Axes.Y) continue;
			if (axis == Axes.Z) continue;
			if (axis == Axes.TIME) continue;
			// calc index of encoded channels
			final long subIndex = workingIndex % dims[i];
			pos[i] = subIndex;
			workingIndex = workingIndex / dims[i];
		}
	}
	
	static long calcIJ1ChannelPos(long[] dims, AxisType[] axes, long[] pos) {
		long multiplier = 1;
		long ij1Pos = 0;
		for (int i = 0; i < axes.length; i++) {
			AxisType axis = axes[i];
			// skip axes we don't encode as channels
			if (axis == Axes.X) continue;
			if (axis == Axes.Y) continue;
			if (axis == Axes.Z) continue;
			if (axis == Axes.TIME) continue;
			ij1Pos += multiplier * pos[i];
			multiplier *= dims[i];
		}
		return ij1Pos;
	}
	
	// -- private helper methods --

	/** Returns true if a {@link Dataset} is backed by {@link PlanarAccess}. */
	private static boolean ij1StorageCompatible(final Dataset ds) {
		return ds.getImgPlus().getImg() instanceof PlanarAccess<?>;
	}

	/**
	 * Returns true if a {@link Dataset} has a type that can be directly
	 * represented in an legacy ImageJ ImagePlus.
	 */
	private static boolean ij1TypeCompatible(final Dataset ds) {
		final RealType<?> type = ds.getType();
		final int bitsPerPix = type.getBitsPerPixel();
		final boolean integer = ds.isInteger();
		final boolean signed = ds.isSigned();

		Object plane;
		if ((bitsPerPix == 8) && !signed && integer) {
			plane = ds.getPlane(0, false);
			if (plane != null && plane instanceof byte[]) return true;
		}
		else if ((bitsPerPix == 16) && !signed && integer) {
			plane = ds.getPlane(0, false);
			if (plane != null && plane instanceof short[]) return true;
		}
		else if ((bitsPerPix == 32) && signed && !integer) {
			plane = ds.getPlane(0, false);
			if (plane != null && plane instanceof float[]) return true;
		}
		return false;
	}

	/* OLD AND TOO SLOW FOR LARGE VIRTUAL IMAGES
	 * 
	 * Determines whether an ImagePlus is an legacy ImageJ binary image (i.e. it
	 * is unsigned 8 bit data with only values 0 & 255 present)
	public static boolean isBinary(final ImagePlus imp) {
		final int numSlices = imp.getStackSize();
		// don't let degenerate images report themselves as binary
		if (numSlices == 0) return false;
		if (numSlices == 1) {
			final ImageProcessor ip = imp.getProcessor();
			if (ip == null) return false; // null possible : treat as numeric data
			return ip.isBinary();
		}
		final int slice = imp.getCurrentSlice();
		final ImageStack stack = imp.getStack();
		// stack cannot be null here as numSlices > 1
		//   and in such cases you always get a non-null processor
		for (int i = 1; i <= numSlices; i++) {
			final ImageProcessor ip = stack.getProcessor(i);
			if (!ip.isBinary()) {
				// fix virtual stack problems
				stack.getProcessor(slice);
				return false;
			}
		}
		// fix virtual stack problems
		stack.getProcessor(slice);
		return true;
	}
	 */

}
