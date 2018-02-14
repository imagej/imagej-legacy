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

import ij.ImagePlus;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;

import org.scijava.Context;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;

/**
 * Creates {@link ImageDisplay}s from {@link ImagePlus}es containing color data.
 * 
 * @author Barry DeZonia
 */
public class ColorDisplayCreator extends AbstractDisplayCreator
{

	// NB - OverlayHarmonizer required because IJ1 plugins can hatch displays
	// while avoiding the Harmonizer. Not required in the Display->ImagePlus
	// direction as Harmonizer always catches that case.

	// -- constructor --

	public ColorDisplayCreator(final Context context) {
		super( context );
	}

	// -- AbstractDisplayCreator methods --

	@Override
	protected ImageDisplay makeDisplay(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		final Dataset ds = getDataset(imp, preferredOrder);
		final ImageDisplay display = harmonizeExceptPixels( imp, ds );

		return display;
	}

	/**
	 * Makes a color {@link Dataset} from an {@link ImagePlus}. Color Datasets
	 * have isRgbMerged() true, channels == 3, and bitsperPixel == 8. Does not
	 * populate the data of the returned Dataset. That is left to other utility
	 * methods. Does not set metadata of Dataset. Throws exceptions if input
	 * ImagePlus is not single channel RGB.
	 */
	@Override
	protected Dataset makeDataset(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		final int c = imp.getNChannels();
		if (c != 1) {
			throw new IllegalArgumentException(
				"can't make a color Dataset from a multichannel ColorProcessor stack");
		}

		Dataset ds = makeGrayDatasetFromColorImp( imp, preferredOrder );
		ds.setRGBMerged(true);
		return ds;
	}
}
