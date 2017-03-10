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
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imagej.space.SpaceUtils;
import net.imglib2.util.Intervals;

/**
 * This class is responsible for harmonizing slider position values (and active
 * plane position) between legacy ImageJ and modern ImageJ.
 * 
 * @author Barry DeZonia
 */
public class PositionHarmonizer implements DisplayHarmonizer {

	/**
	 * Updates the given {@link ImageDisplay}'s position to match that of its
	 * paired {@link ImagePlus}.
	 */
	@Override
	public void updateDisplay(ImageDisplay disp, ImagePlus imp) {
		final long[] dimensions = Intervals.dimensionsAsLongArray(disp);
		final AxisType[] axes = SpaceUtils.getAxisTypes(disp);
		final long[] workspace = new long[dimensions.length];
		fillModernIJPosition(disp, imp, dimensions, axes, workspace);
		for (int i = 0; i < axes.length; i++) {
			final long pos = workspace[i];
			disp.setPosition(pos, i);
		}
	}

	/**
	 * Updates the given {@link ImagePlus}'s position to match that of its
	 * paired {@link ImageDisplay}.
	 */
	@Override
	public void updateLegacyImage(ImageDisplay disp, ImagePlus imp) {
		// When this is called we know that we have a IJ1 compatible display. So we
		// can make assumptions about dimensional sizes re: safe casting.
		final int cPos = (int) calcIJ1ChannelPos(disp);
		final int zPos = (int) disp.getLongPosition(Axes.Z);
		final int tPos = (int) disp.getLongPosition(Axes.TIME);
		imp.setPosition(cPos+1, zPos+1, tPos+1); 
	}

	// -- helpers --
	
	private long calcIJ1ChannelPos(ImageDisplay disp) {
		final long[] dims = Intervals.dimensionsAsLongArray(disp);
		final AxisType[] axes = SpaceUtils.getAxisTypes(disp);
		final long[] pos = new long[axes.length];
		for (int i = 0; i < axes.length; i++)
			pos[i] = disp.getLongPosition(i);
		return LegacyUtils.calcIJ1ChannelPos(dims, axes, pos);
	}
	
	private void fillModernIJPosition(ImageDisplay disp, ImagePlus imp,
		long[] dimensions, AxisType[] axes, long[] workspace)
	{
		fillIndex(disp, Axes.X, workspace);
		fillIndex(disp, Axes.Y, workspace);
		fillIndex(disp, Axes.Z, workspace);
		fillIndex(disp, Axes.TIME, workspace);
		LegacyUtils.fillChannelIndices(
			dimensions, axes, imp.getChannel()-1, workspace);
	}
	
	private void fillIndex(ImageDisplay disp, AxisType axis, long[] workspace) {
		final int index = disp.dimensionIndex(axis);
		if (index != -1) workspace[index] = disp.getLongPosition(index); 
	}
}
