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
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;

/**
 * Supports bidirectional synchronization between color {@link ImagePlus}es and
 * merged {@link Dataset}s.
 * 
 * @author Barry DeZonia
 */
public class ColorPixelHarmonizer implements DataHarmonizer {

	// -- instance variables --

	private double[] savedPlane;
	private int savedPos;

	// -- public api --

	// NOTE: to propagate a VirtualStack's first plane pixel changes we save it
	// early in the harmonization process and refer to it later. This code is part
	// of that process

	/**
	 * Users of ColorPixelHarmonizer can pass it a copy of the current plane of
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
	 * Assigns the data values of a color {@link Dataset} from a paired
	 * {@link ImagePlus}. Assumes the Dataset and ImagePlus have compatible
	 * dimensions and are both of type color. Gets values via
	 * {@link ImageProcessor}::get(). Does not change the Dataset's metadata.
	 */
	@Override
	public void updateDataset(final Dataset ds, final ImagePlus imp) {
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int cIndex = ds.dimensionIndex(Axes.CHANNEL);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final int tIndex = ds.dimensionIndex(Axes.TIME);
		final int xSize = imp.getWidth();
		final int ySize = imp.getHeight();
		final int cSize = imp.getNChannels();
		final int zSize = imp.getNSlices();
		final int tSize = imp.getNFrames();
		final ImageStack stack = imp.getStack();
		int imagejPlaneNumber = 1;
		final RandomAccess<? extends RealType<?>> accessor =
			ds.getImgPlus().randomAccess();
		int slice = imp.getCurrentSlice();
		for (int t = 0; t < tSize; t++) {
			if (tIndex >= 0) accessor.setPosition(t, tIndex);
			for (int z = 0; z < zSize; z++) {
				if (zIndex >= 0) accessor.setPosition(z, zIndex);
				for (int c = 0; c < cSize; c++) {
					final ImageProcessor proc = stack.getProcessor(imagejPlaneNumber++);
					// TEMP HACK THAT FIXES VIRT STACK PROB BUT SLOW
					// imp.setPosition(planeNum - 1);
					for (int y = 0; y < ySize; y++) {
						accessor.setPosition(y, yIndex);
						for (int x = 0; x < xSize; x++) {
							accessor.setPosition(x, xIndex);
							// NOTE: to propagate a VirtualStack's first plane pixel changes
							// we save it early in the harmonization process and refer to it
							// later. This code is part of that process
							final int value;
							if (savedPos == imagejPlaneNumber - 1) {
								int index = xSize * y + x;
								value = (int) savedPlane[index];
							}
							else {
								value = proc.get(x, y);
							}
							final int rValue = (value >> 16) & 0xff;
							final int gValue = (value >> 8) & 0xff;
							final int bValue = (value >> 0) & 0xff;
							accessor.setPosition(c * 3, cIndex);
							accessor.get().setReal(rValue);
							accessor.fwd(cIndex);
							accessor.get().setReal(gValue);
							accessor.fwd(cIndex);
							accessor.get().setReal(bValue);
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
	 * Assigns the data values of a color {@link ImagePlus} from a paired
	 * {@link Dataset}. Assumes the Dataset and ImagePlus have compatible
	 * dimensions and that the data planes are not directly mapped. Also assumes
	 * that the Dataset has isRGBMerged() true. Sets values via
	 * {@link ImageProcessor}::set(). Does not change the ImagePlus' metadata.
	 */
	@Override
	public void updateLegacyImage(final Dataset ds, final ImagePlus imp) {
		final int xIndex = ds.dimensionIndex(Axes.X);
		final int yIndex = ds.dimensionIndex(Axes.Y);
		final int cIndex = ds.dimensionIndex(Axes.CHANNEL);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final int xSize = imp.getWidth();
		final int ySize = imp.getHeight();
		final int cSize = imp.getNChannels();
		final int zSize = imp.getNSlices();
		final int tSize = imp.getNFrames();
		int tIndex = Math.max(cIndex, zIndex) + 1;
		if (tIndex == 1) tIndex = yIndex + 1;
		final ImageStack stack = imp.getStack();
		int imagejPlaneNumber = 1;
		final RandomAccess<? extends RealType<?>> accessor =
			ds.getImgPlus().randomAccess();
		int slice = imp.getCurrentSlice();
		final long[] tPos = new long[ds.numDimensions() - tIndex];
		for (int i = tIndex; i<ds.numDimensions(); i++) {
			tPos[i - tIndex] = ds.dimension(i);
		}
		for (int t = 0; t < tSize; t++) {
			updatePosition(accessor, tPos, t, tIndex);
			for (int z = 0; z < zSize; z++) {
				if (zIndex >= 0) accessor.setPosition(z, zIndex);
				for (int c = 0; c < cSize; c++) {
					final ImageProcessor proc = stack.getProcessor(imagejPlaneNumber++);
					if (!ds.isRGBMerged() && cIndex >= 0) {
						accessor.setPosition(c, cIndex);
					}
					// TEMP HACK THAT FIXES VIRT STACK PROB BUT SLOW
					// imp.setPosition(planeNum - 1);
					for (int y = 0; y < ySize; y++) {
						accessor.setPosition(y, yIndex);
						for (int x = 0; x < xSize; x++) {
							accessor.setPosition(x, xIndex);

							int intValue = 0;
							if (ds.isRGBMerged()) {
								accessor.setPosition(3 * c, cIndex);
								final int rValue = ((int) accessor.get().getRealDouble()) & 0xff;

								accessor.fwd(cIndex);
								final int gValue = ((int) accessor.get().getRealDouble()) & 0xff;

								accessor.fwd(cIndex);
								final int bValue = ((int) accessor.get().getRealDouble()) & 0xff;

								intValue =
										(0xff << 24) | (rValue << 16) | (gValue << 8) | (bValue);
							}
							else {
								intValue = ((int)accessor.get().getRealDouble());
							}

							proc.set(x, y, intValue);
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
	 * Sets the positions of the given accessor, from [start, start +
	 * lengths.length], by converting the given index to a position, using the
	 * given lengths array to convert from raster to position.
	 */
	private void updatePosition(RandomAccess<? extends RealType<?>> accessor,
		long[] lengths, int index, int start)
	{
		// IntervalIndexer throws an exception if given an empty array.
		if (lengths.length > 0) {
			long[] position = new long[lengths.length];
			IntervalIndexer.indexToPosition(index, lengths, position);
			for (int i=0; i<lengths.length; i++) {
				int dim = i + start;
				accessor.setPosition(position[i], dim);
			}
		}
	}
}
