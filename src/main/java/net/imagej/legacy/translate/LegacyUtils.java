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
import ij.WindowManager;
import ij.macro.Interpreter;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.Axis;
import net.imagej.axis.AxisType;
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
	 * Returns the number of planes needed in legacy ImageJ to represent all
	 * the axes of a modern ImageJ Dataset. Incompatible modern axes are encoded
	 * as extra planes in the legacy ImageJ image.
	 */
	static long ij1PlaneCount(ImgPlus imgPlus, final AxisType allowList) {
		long planeCount = 1;
		int axisIndex = 0;
		for ( int i = 0; i < imgPlus.numDimensions(); i++) {
			Axis axisType = imgPlus.axis(i);
			final long axisSize = imgPlus.dimension(axisIndex++);
			// we want to skip the X,Y,C,Z,T axes, unless that axis is the white list.
			// this will cause planeCount to be the product of the allowList axis and
			// all other axes
			if (axisType == allowList) {} // DON'T continue
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
}
