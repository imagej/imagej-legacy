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
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imglib2.img.VirtualStackAdapter;

import org.scijava.Context;

/**
 * Creates {@link ImageDisplay}s containing gray data values from
 * {@link ImagePlus}es.
 * 
 * @author Barry DeZonia
 */
public class GrayDisplayCreator extends AbstractDisplayCreator
{

	// NB - OverlayHarmonizer required because IJ1 plugins can hatch displays
	// while avoiding the Harmonizer. Not required in the Display->ImagePlus
	// direction as Harmonizer always catches that case.

	// -- constructor --

	public GrayDisplayCreator(final Context context) {
		super(context);
	}

	// -- AbstractDisplayCreator methods --

	@Override
	protected Dataset makeDataset(ImagePlus imp, AxisType[] preferredOrder) {
		if (imp.getType() == ImagePlus.COLOR_RGB) {
			return makeGrayDatasetFromColorImp(imp, preferredOrder);
		}
		else {
			return makeExactDataset(imp, preferredOrder);
		}
	}

	// -- private interface --

	/**
	 * Makes a planar {@link Dataset} whose dimensions match a given
	 * {@link ImagePlus}. Assumes it will never be called with
	 * any kind of color ImagePlus. Does not set metadata of Dataset.
	 */
	private Dataset makeExactDataset(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		ImgPlus imgPlus = VirtualStackAdapter.wrap(imp);
		final Dataset ds = datasetService.create(imgPlus);
		DatasetUtils.initColorTables(ds);
		return ds;
	}
}
