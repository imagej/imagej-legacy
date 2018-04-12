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

import ij.CompositeImage;
import ij.ImagePlus;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.display.ColorMode;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;

import net.imglib2.img.display.imagej.ArrayImgToVirtualStack;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
import net.imglib2.img.display.imagej.PlanarImgToVirtualStack;
import net.imglib2.type.numeric.RealType;
import org.scijava.AbstractContextual;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

/**
 * Creates {@link ImagePlus}es from {@link ImageDisplay}.
 * 
 * @author Barry DeZonia
 * @author Matthias Arzt
 */
public class ImagePlusCreator extends AbstractContextual
{

	// -- instance variables --

	private final ColorTableHarmonizer colorTableHarmonizer;
	private final PositionHarmonizer positionHarmonizer;
	private final NameHarmonizer nameHarmonizer;

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private LogService log;

	// -- public interface --

	public ImagePlusCreator(final Context context) {
		setContext(context);
		colorTableHarmonizer = new ColorTableHarmonizer(imageDisplayService);
		positionHarmonizer = new PositionHarmonizer();
		nameHarmonizer = new NameHarmonizer();
	}

	public ImagePlus createLegacyImage(final ImageDisplay display) {
		final Dataset dataset = imageDisplayService.getActiveDataset(display);
		return createLegacyImage(dataset, display);
	}

	public ImagePlus createLegacyImage(final Dataset ds) {
		return createLegacyImage(ds, null);
	}

	public ImagePlus createLegacyImage(final Dataset dataset,
		final ImageDisplay display)
	{
		if (dataset == null) return null;
		ImagePlus imp = createImagePlus( dataset );
		ImagePlusCreatorUtils.setMetadata( dataset, imp );
		imp = ImagePlusCreatorUtils.optionalMakeComposite( dataset, imp );
		if (display != null) {
			if (shouldBeComposite(display, dataset, imp)) {
				imp = makeCompositeImage(imp);
			}
			colorTableHarmonizer.updateLegacyImage(display, imp);
			positionHarmonizer.updateLegacyImage(display, imp);
			nameHarmonizer.updateLegacyImage(display, imp);
		}

		return imp;
	}

	private static ImagePlus createImagePlus( Dataset dataset )
	{
		ImgPlus< ? extends RealType< ? > > imgPlus = dataset.getImgPlus();
		if( PlanarImgToVirtualStack.isSupported( imgPlus ) )
			return PlanarImgToVirtualStack.wrap( imgPlus );
		if( ArrayImgToVirtualStack.isSupported( imgPlus ) )
			return ArrayImgToVirtualStack.wrap( imgPlus );
		return ImgToVirtualStack.wrap( imgPlus, dataset.isRGBMerged() );
	}

	// -- private interface --

	private boolean shouldBeComposite(final ImageDisplay display,
		final Dataset ds, final ImagePlus imp)
	{
		final int channels = imp.getNChannels();
		if (channels < 2 || channels > 7) return false;
		final DatasetView view = imageDisplayService.getActiveDatasetView(display);
		if (view != null && view.getColorMode() == ColorMode.COMPOSITE) return true;
		if (ds.getCompositeChannelCount() == 1) return false;
		return true;
	}

	/**
	 * Makes a {@link CompositeImage} that wraps a given {@link ImagePlus} and
	 * sets channel LUTs to match how modern ImageJ displays the given paired
	 * {@link Dataset}. Assumes given ImagePlus has channels in range 2..7 and
	 * that if Dataset View has ColorTables defined there is one per channel.
	 */
	// TODO - last assumption may be bad. If I have a 6 channel Dataset with
	// compos chann count == 2 then maybe I only have 2 or 3 ColorTables. Is
	// this configuration even valid. If so then what to do for translation?
	private CompositeImage makeCompositeImage(final ImagePlus imp) {
		return new CompositeImage(imp, CompositeImage.COMPOSITE);
	}

}
