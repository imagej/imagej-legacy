/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2014 Board of Regents of the University of
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
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.LegacyService;
import net.imglib2.meta.AxisType;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

/**
 * The default {@link ImageTranslator} between legacy and modern ImageJ image
 * structures. It delegates to the appropriate more specific translators based
 * on the type of data being translated.
 * 
 * @author Barry DeZonia
 * @author Curtis Rueden
 */
public class DefaultImageTranslator extends AbstractContextual implements
	ImageTranslator
{

	private final DisplayCreator colorDisplayCreator;
	private final DisplayCreator grayDisplayCreator;
	private final ImagePlusCreator colorImagePlusCreator;
	private final ImagePlusCreator grayImagePlusCreator;

	private final LegacyService legacyService;

	@Parameter
	private ImageDisplayService imageDisplayService;

	public DefaultImageTranslator(final LegacyService legacyService) {
		final Context context = legacyService.getContext();
		context.inject(this);
		this.legacyService = legacyService;
		colorDisplayCreator = new ColorDisplayCreator(context);
		grayDisplayCreator = new GrayDisplayCreator(context);
		colorImagePlusCreator = new ColorImagePlusCreator(context);
		grayImagePlusCreator = new GrayImagePlusCreator(context);
	}

	/**
	 * Creates a {@link ImageDisplay} from an {@link ImagePlus}. Shares planes of
	 * data when possible.
	 */
	@Override
	public ImageDisplay createDisplay(final ImagePlus imp) {
		
		return createDisplay(imp, LegacyUtils.getPreferredAxisOrder());
	}

	/**
	 * Creates a {@link ImageDisplay} from an {@link ImagePlus}. Shares planes of
	 * data when possible. Builds ImageDisplay with preferred Axis ordering.
	 */
	@Override
	public ImageDisplay createDisplay(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{

		if ((imp.getType() == ImagePlus.COLOR_RGB) && (imp.getNChannels() == 1)) {
			return colorDisplayCreator.createDisplay(imp, preferredOrder);
		}

		return grayDisplayCreator.createDisplay(imp, preferredOrder);
	}

	/**
	 * Creates an {@link ImagePlus} from a {@link ImageDisplay}. Shares planes of
	 * data when possible.
	 */
	@Override
	public ImagePlus createLegacyImage(final ImageDisplay display) {
		final Dataset ds = imageDisplayService.getActiveDataset(display);
		return createLegacyImage(ds, display);
	}

	@Override
	public ImagePlus createLegacyImage(final Dataset ds) {
		return createLegacyImage(ds, null);
	}

	@Override
	public ImagePlus createLegacyImage(final Dataset ds,
		final ImageDisplay display)
	{
		ImagePlus imp = null;
		// Can only create an ImagePlus if we have 3 dimensions (XYC) as
		// RGB stacks are not supported in IJ 1.x
		if (ds.isRGBMerged() && ds.getImgPlus().numDimensions() <= 3) {
			imp = colorImagePlusCreator.createLegacyImage(ds, display);
		}
		else {
			imp = grayImagePlusCreator.createLegacyImage(ds, display);
		}

		legacyService.getImageMap().registerLegacyImage(imp);

		return imp;
	}

}
