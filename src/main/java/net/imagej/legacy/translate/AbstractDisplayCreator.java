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
import ij.io.FileInfo;

import net.imagej.Dataset;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.LegacyImageMap;

import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;

/**
 * Abstract superclass for DisplayCreator implementations. Ensures
 * proper linkage between {@link Dataset} and {@link ImagePlus} instances.
 *
 * @author Mark Hiner
 */
public abstract class AbstractDisplayCreator extends AbstractContextual
{

	@Parameter
	private ImageDisplayService imageDisplayService;

	private final ColorTableHarmonizer colorTableHarmonizer;
	private final MetadataHarmonizer metadataHarmonizer;
	private final CompositeHarmonizer compositeHarmonizer;
	private final OverlayHarmonizer overlayHarmonizer;
	private final PositionHarmonizer positionHarmonizer;
	private final NameHarmonizer nameHarmonizer;

	@Parameter
	private DisplayService displayService;

	public AbstractDisplayCreator( final Context context )
	{
		setContext(context);
		nameHarmonizer = new NameHarmonizer();
		metadataHarmonizer = new MetadataHarmonizer();
		overlayHarmonizer = new OverlayHarmonizer(context);
		positionHarmonizer = new PositionHarmonizer();
		compositeHarmonizer = new CompositeHarmonizer();
		colorTableHarmonizer = new ColorTableHarmonizer(imageDisplayService);
	}

	public ImageDisplay createDisplay(final ImagePlus imp) {

		return createDisplay(imp, LegacyUtils.getPreferredAxisOrder());
	}

	public ImageDisplay createDisplay(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		return makeDisplay(imp, preferredOrder);
	}

	/**
	 * @return A {@link Dataset} appropriate for the given {@link ImagePlus}
	 */
	protected Dataset getDataset(final ImagePlus imp,
		final AxisType[] preferredOrder)
	{
		final Dataset ds = makeDataset(imp, preferredOrder);
		ds.getProperties().put(LegacyImageMap.IMP_KEY, imp);
		final FileInfo fileInfo = imp.getOriginalFileInfo();
		String source = "";
		if (fileInfo == null) {
			// If no original file info, just use the title. This may be the case
			// when an ImagePlus is created as the output of a command.
			source = imp.getTitle();
		}
		else {
			if (fileInfo.url == null || fileInfo.url.isEmpty()) {
				source = fileInfo.directory + fileInfo.fileName;
			}
			else {
				source = fileInfo.url;
			}
		}
		ds.getImgPlus().setSource(source);
		return ds;
	}

	protected ImageDisplay harmonizeExceptPixels( ImagePlus imp, Dataset ds )
	{
		metadataHarmonizer.updateDataset(ds, imp);
		compositeHarmonizer.updateDataset(ds, imp);

		// CTR FIXME - add imageDisplayService.createImageDisplay method?
		// returns null if it cannot find an ImageDisplay-compatible display?
		final ImageDisplay display =
			(ImageDisplay) displayService.createDisplay(ds.getName(), ds);

		colorTableHarmonizer.updateDisplay(display, imp);
		// NB - correct thresholding behavior requires overlay harmonization after
		// color table harmonization
		overlayHarmonizer.updateDisplay(display, imp);
		positionHarmonizer.updateDisplay(display, imp);
		nameHarmonizer.updateDisplay(display, imp);
		return display;
	}

	/**
	 * @return A {@link Dataset} appropriate for the given {@link ImagePlus}
	 */
	protected abstract Dataset makeDataset(final ImagePlus imp,
		final AxisType[] preferredOrder);

	/**
	 * @return An {@link ImageDisplay} created from the given {@link ImagePlus}
	 */
	protected abstract ImageDisplay makeDisplay(final ImagePlus imp,
		final AxisType[] preferredOrder);
}
