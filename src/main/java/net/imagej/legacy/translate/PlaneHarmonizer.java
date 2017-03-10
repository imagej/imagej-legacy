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
import net.imagej.Dataset;
import net.imagej.Extents;
import net.imagej.Position;
import net.imagej.axis.Axes;

import org.scijava.log.LogService;

/**
 * Synchronizes internal plane reference values between a {@link Dataset} and an
 * {@link ImagePlus}. After synchronization each one of them will share the same
 * plane memory references.
 * 
 * @author Barry DeZonia
 */
public class PlaneHarmonizer implements DataHarmonizer {

	private final LogService log;

	public PlaneHarmonizer(LogService log) {
		this.log = log;
	}

	/**
	 * Assigns a planar {@link Dataset}'s plane references to match those of a
	 * given {@link ImagePlus}. Assumes input Dataset and ImagePlus match in
	 * dimensions and backing type.
	 */
	@Override
	public void updateDataset(final Dataset ds, final ImagePlus imp) {
		final int cCount = imp.getNChannels();
		final int zCount = imp.getNSlices();
		final int tCount = imp.getNFrames();

		final int cIndex = ds.dimensionIndex(Axes.CHANNEL);
		final int zIndex = ds.dimensionIndex(Axes.Z);
		final int tIndex = ds.dimensionIndex(Axes.TIME);

		final ImageStack stack = imp.getStack();

		final long[] planeDims = new long[ds.numDimensions() - 2];
		for (int i = 0; i < planeDims.length; i++)
			planeDims[i] = ds.dimension(i + 2);
		final Position planePos = new Extents(planeDims).createPosition();

		// copy planes by reference
		boolean changes = false;
		if (imp.getStackSize() == 1) {
			changes |= ds.setPlaneSilently(0, imp.getProcessor().getPixels());
		}
		else {
			int stackPosition = 1;
			for (int t = 0; t < tCount; t++) {
				if (tIndex >= 0) planePos.setPosition(t, tIndex - 2);
				for (int z = 0; z < zCount; z++) {
					if (zIndex >= 0) planePos.setPosition(z, zIndex - 2);
					for (int c = 0; c < cCount; c++) {
						if (cIndex >= 0) planePos.setPosition(c, cIndex - 2);
						final Object plane = stack.getPixels(stackPosition++);
						if (plane == null) {
							log.error("Could not extract plane from ImageStack position: " +
								(stackPosition - 1));
						}
						final int planeNum = (int) planePos.getIndex();
						changes |= ds.setPlaneSilently(planeNum, plane);
					}
				}
			}
		}
		if (changes) ds.update();
	}

	/**
	 * Assigns the plane references of an {@link ImagePlus}' {@link ImageStack} to
	 * match those of a given {@link Dataset}. Assumes input Dataset and ImagePlus
	 * match in dimensions and backing type. Throws an exception if Dataset axis 0
	 * is not X or Dataset axis 1 is not Y.
	 */
	@Override
	public void updateLegacyImage(final Dataset ds, final ImagePlus imp) {
		final int[] dimIndices = new int[5];
		final int[] dimValues = new int[5];
		LegacyUtils.getImagePlusDims(ds, dimIndices, dimValues);
		LegacyUtils.assertXYPlanesCorrectlyOriented(dimIndices);

		final int cCount = dimValues[2];
		final int zCount = dimValues[3];
		final int tCount = dimValues[4];

		final int cIndex = dimIndices[2];
		final int zIndex = dimIndices[3];
		final int tIndex = dimIndices[4];

		final ImageStack stack = imp.getStack();

		final long[] planeDims = new long[ds.numDimensions() - 2];
		for (int i = 0; i < planeDims.length; i++)
			planeDims[i] = ds.dimension(i + 2);
		final Extents extents = new Extents(planeDims);
		final Position planePos = extents.createPosition();

		// copy planes by reference

		int currSlice = imp.getCurrentSlice();
		Object plane = null;
		int stackPosition = 1;
		for (int t = 0; t < tCount; t++) {
			if (tIndex >= 0) planePos.setPosition(t, tIndex - 2);
			for (int z = 0; z < zCount; z++) {
				if (zIndex >= 0) planePos.setPosition(z, zIndex - 2);
				for (int c = 0; c < cCount; c++) {
					if (cIndex >= 0) planePos.setPosition(c, cIndex - 2);
					final int planeNum = (int) planePos.getIndex();
					plane = ds.getPlane(planeNum, false);
					if (plane == null) {
						log.error(message("Can't extract plane from Dataset ", c, z, t));
					}
					stack.setPixels(plane, stackPosition);
					if (stackPosition == currSlice) imp.getProcessor().setPixels(plane);
					stackPosition++;
				}
			}
		}
	}

	// -- private interface --

	/** Formats an error message. */
	private String message(final String message, final long c, final long z,
		final long t)
	{
		return message + ": c=" + c + ", z=" + z + ", t=" + t;
	}
}
