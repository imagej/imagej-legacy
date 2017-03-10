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
import ij.ImageStack;
import ij.process.ImageProcessor;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.space.SpaceUtils;
import net.imglib2.RandomAccess;
import net.imglib2.util.Intervals;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.util.IntervalIndexer;

/**
 * Supports bidirectional synchronization between {@link ImagePlus}es and gray
 * {@link Dataset}s. Single channel color {@link ImagePlus}es are not supported
 * here. But multichannel color {@link ImagePlus}es are handled and treated as
 * gray data.
 * 
 * @author Barry DeZonia
 */
public class GrayPixelHarmonizer implements DataHarmonizer {

	// -- instance variables --

	private double[] savedPlane;
	private int savedPos;

	// -- public api --

	// NOTE: to propagate a VirtualStack's first plane pixel changes we save it
	// early in the harmonization process and refer to it later. This code is part
	// of that process

	/**
	 * Users of GrayPixelHarmonizer can pass it a copy of the current plane of
	 * pixels of an ImagePlus.
	 * 
	 * @param pos Slice number of the current plane
	 * @param plane Pixels copy of the current plane
	 */
	public void savePlane(int pos, double[] plane) {
		savedPos = pos;
		savedPlane = plane;
	}

	/**
	 * Assigns the data values of a {@link Dataset} from a paired
	 * {@link ImagePlus}. Assumes the Dataset and ImagePlus have compatible
	 * dimensions and that the data planes are not directly mapped. Gets values
	 * via {@link ImageProcessor}::getf(). In cases where there is a narrowing of
	 * data into modern ImageJ types the data is range clamped. Does not change
	 * the Dataset's metadata.
	 */
	@Override
	public void updateDataset(final Dataset ds, final ImagePlus imp) {
		final RealType<?> type = ds.getType();
		final double typeMin = type.getMinValue();
		final double typeMax = type.getMaxValue();
		final boolean signed16BitData = type instanceof ShortType;
		final RandomAccess<? extends RealType<?>> accessor =
			ds.getImgPlus().randomAccess();
		final long[] dims = Intervals.dimensionsAsLongArray(ds);
		final AxisType[] axes = SpaceUtils.getAxisTypes(ds);
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final int tIndex = ds.dimensionIndex(Axes.TIME);
		final int xSize = imp.getWidth();
		final int ySize = imp.getHeight();
		final int zSize = imp.getNSlices();
		final int tSize = imp.getNFrames();
		final int cSize = imp.getNChannels();
		final ImageStack stack = imp.getStack();
		int planeNum = 1;
		final long[] pos = new long[dims.length];
		int slice = imp.getCurrentSlice();
		for (int t = 0; t < tSize; t++) {
			if (tIndex >= 0) pos[tIndex] = t;
			for (int z = 0; z < zSize; z++) {
				if (zIndex >= 0) pos[zIndex] = z;
				for (int c = 0; c < cSize; c++) {
					LegacyUtils.fillChannelIndices(dims, axes, c, pos);
					ImageProcessor proc = stack.getProcessor(planeNum++);
					// TEMP HACK THAT FIXES VIRT STACK PROB BUT SLOW
					// imp.setPosition(planeNum - 1);
					for (int x = 0; x < xSize; x++) {
						if (xIndex >= 0) pos[xIndex] = x;
						for (int y = 0; y < ySize; y++) {
							if (yIndex >= 0) pos[yIndex] = y;
							accessor.setPosition(pos);
							// NOTE: to propagate a VirtualStack's first plane pixel changes
							// we save it early in the harmonization process and refer to it
							// later. This code is part of that process
							double value;
							if (savedPos == planeNum - 1) {
								int index = xSize * y + x;
								value = savedPlane[index];
							}
							else {
								value = proc.getf(x, y);
							}
							if (signed16BitData) value -= 32768.0;
							if (value < typeMin) value = typeMin;
							else if (value > typeMax) value = typeMax;
							accessor.get().setReal(value);
						}
					}
				}
			}
		}
		// NOTE: the stack.getProcessor() calls that have been called so far have
		// changed the current plane's pixels for virtual stacks. So reset pixels
		// to correct plane's values
		stack.getProcessor(slice);

		ds.update();
	}

	/**
	 * Assigns the data values of an {@link ImagePlus} from a paired
	 * {@link Dataset}. Assumes the Dataset and ImagePlus are not directly mapped.
	 * It is possible that multiple modern ImageJ axes are encoded as a single set
	 * of channels in the ImagePlus. Sets values via {@link ImageProcessor}
	 * ::setf(). Some special case code is in place to assure that BitType images
	 * go to legacy ImageJ as 0/255 value images. Does not change the ImagePlus'
	 * metadata.
	 */
	@Override
	public void updateLegacyImage(final Dataset ds, final ImagePlus imp) {
		final RealType<?> type = ds.getType();
		final boolean signed16BitData = type instanceof ShortType;
		final boolean bitData = type instanceof BitType;
		final RandomAccess<? extends RealType<?>> accessor =
			ds.getImgPlus().randomAccess();
		final long[] dims = Intervals.dimensionsAsLongArray(ds);
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final int cIndex = ds.dimensionIndex(Axes.CHANNEL);
		final int xSize = imp.getWidth();
		final int ySize = imp.getHeight();
		final int zSize = imp.getNSlices();
		final int tSize = imp.getNFrames();
		final int cSize = imp.getNChannels();
		int tIndex = Math.max(yIndex, zIndex) + 1;
		final ImageStack stack = imp.getStack();
		int planeNum = 1;
		final long[] pos = new long[dims.length];
		int slice = imp.getCurrentSlice();
		final long[] tPos = new long[ds.numDimensions() - tIndex];
		for (int i = tIndex; i<ds.numDimensions(); i++) {
			tPos[i - tIndex] = ds.dimension(i);
		}
		for (int t = 0; t < tSize; t++) {
			updatePosition(pos, tPos, t, tIndex);
			for (int z = 0; z < zSize; z++) {
				if (zIndex >= 0) pos[zIndex] = z;
				for (int c = 0; c < cSize; c++) {
					if (cIndex >= 0) pos[cIndex] = c;
					final ImageProcessor proc = stack.getProcessor(planeNum++);
					// TEMP HACK THAT FIXES VIRT STACK PROB BUT SLOW
					// imp.setPosition(planeNum - 1);
					for (int x = 0; x < xSize; x++) {
						if (xIndex >= 0) pos[xIndex] = x;
						for (int y = 0; y < ySize; y++) {
							if (yIndex >= 0) pos[yIndex] = y;
							accessor.setPosition(pos);
							double value = accessor.get().getRealDouble();
							if (signed16BitData) value += 32768.0;
							else if (bitData) if (value > 0) value = 255;
							proc.setf(x, y, (float)value);
						}
					}
				}
			}
		}
		// NOTE: the stack.getProcessor() calls that have been called so far have
		// changed the current plane's pixels for virtual stacks. So reset pixels
		// to correct plane's values
		stack.getProcessor(slice);
	}

	/**
	 * Sets the positions of the given dims array, from [start, start +
	 * lengths.length], by converting the given index to a position, using the
	 * given lengths array to convert from raster to position.
	 */
	private void updatePosition(long[] position, long[] tPos, int index, int start) {
		// IntervalIndexer throws an exception if given an empty array.
		if (tPos.length > 0) {
			long[] temp = new long[tPos.length];
			IntervalIndexer.indexToPosition(index, tPos, temp);
			for (int i=0; i<tPos.length; i++) {
				int dim = i + start;
				position[dim] = temp[i];
			}
		}
	}

}
