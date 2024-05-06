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

import ij.CompositeImage;
import ij.ImagePlus;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.img.display.imagej.ArrayImgToVirtualStack;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.img.display.imagej.ImgToVirtualStack;
import net.imglib2.img.display.imagej.PlanarImgToVirtualStack;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

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
		imp = optionalMakeComposite( dataset, imp );
		if (display != null) {
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
		if( Util.getTypeFromInterval( imgPlus ) instanceof BitType ) {
			@SuppressWarnings("unchecked")
			final ImgPlus<BitType> bitImgPlus = (ImgPlus<BitType>) imgPlus;
			return ImgToVirtualStack.wrapAndScaleBitType( bitImgPlus );
		}
		if( dataset.isRGBMerged() && ImgPlusViews.canFuseColor( imgPlus ) )
			return ImgToVirtualStack.wrap( ImgPlusViews.fuseColor( imgPlus ) );
		return ImgToVirtualStack.wrap( imgPlus );
	}

	// -- private interface --

	private static ImagePlus optionalMakeComposite( Dataset ds, ImagePlus imp )
	{
		/*
		 * ImageJ 1.x will use a StackWindow *only* if there is more than one channel.
		 * So unfortunately, we cannot consistently return a composite image here. We
		 * have to continue to deliver different data types that require specific case
		 * logic in any handler.
		 */
		// NB: ColorTableHarmonizer crashes, if it gets a CompositeImage but the Dataset has no CHANNEL axis.
		if ( imp.getType() != ImagePlus.COLOR_RGB && imp.getStackSize() > 1 && ds.axis( Axes.CHANNEL ).isPresent() )
			return new CompositeImage(imp, CompositeImage.COMPOSITE);
		return imp;
	}
}
