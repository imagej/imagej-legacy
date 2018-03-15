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
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJVirtualStackARGB;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import net.imglib2.view.Views;
import net.imglib2.view.composite.Composite;
import net.imglib2.view.composite.CompositeIntervalView;
import net.imglib2.view.composite.GenericComposite;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: virtual stack support is minorly problematic. Imglib has vstack impls
// but they use 1-pixel to 1-pixel converters. However in IJ2 in this case we
// have a 3 channel image going to a 1-channel rgb image. So to support virtual
// stacks I can't use Imglib's vstacks. I need to create a special one here
// that converts 3 channels to 1 ARGB. But also note that ARGB is not a RealType
// and thus cannot be the basis of a Dataset.
// Note I have written a FakeVirtualStack class to support this case.

/**
 * Creates {@link ImagePlus}es from {@link ImageDisplay}s containing color
 * merged data.
 * 
 * @author Barry DeZonia
 */
public class ColorImagePlusCreator extends AbstractImagePlusCreator
{

	// -- instance variables --

	private final MetadataHarmonizer metadataHarmonizer;
	private final PositionHarmonizer positionHarmonizer;
	private final NameHarmonizer nameHarmonizer;
	private final ColorTableHarmonizer colorTableHarmonizer;

	@Parameter
	private ImageDisplayService imageDisplayService;

	// -- public interface --

	public ColorImagePlusCreator(final Context context) {
		setContext(context);
		metadataHarmonizer = new MetadataHarmonizer();
		positionHarmonizer = new PositionHarmonizer();
		nameHarmonizer = new NameHarmonizer();
		colorTableHarmonizer =
			new ColorTableHarmonizer(context.getService(ImageDisplayService.class));

	}
	
	/**
	 * Creates a color {@link ImagePlus} from a color {@link ImageDisplay}.
	 * Expects input expects input ImageDisplay to have isRgbMerged() set with 3
	 * channels of unsigned byte data.
	 */
	public ImagePlus createLegacyImage(final ImageDisplay display) {
		final Dataset dataset = imageDisplayService.getActiveDataset(display);
		return createLegacyImage(dataset, display);
	}

	public ImagePlus createLegacyImage(Dataset ds) {
		return createLegacyImage(ds, null);
	}

	public ImagePlus createLegacyImage(Dataset ds, ImageDisplay display) {
		if (ds == null) return null;
		final ImagePlus imp = cellImgCase(ds);
		metadataHarmonizer.updateLegacyImage(ds, imp);

		populateCalibrationData(imp, ds);

		if (display != null) {
			positionHarmonizer.updateLegacyImage(display, imp);
			nameHarmonizer.updateLegacyImage(display, imp);
			colorTableHarmonizer.updateLegacyImage(display, imp);
		}

		return imp;
	}
	// -- private interface --

	private ImagePlus cellImgCase(Dataset ds) {
		if(ds.isRGBMerged())
			return makeImagePlus(ds, new ImageJVirtualStackARGB<>( collapseColorAxis( ds ), this::convertToColor ));
		else
			return makeImagePlus( ds, new GrayImagePlusCreator( context() ).createVirtualStack( ds ) );
	}

	private CompositeIntervalView< RealType< ? >, ? extends GenericComposite< RealType< ? > > > collapseColorAxis( Dataset ds )
	{
		return Views.collapse( makeChannelLastDimension( ds ) );
	}

	private RandomAccessibleInterval< RealType< ? > > makeChannelLastDimension( Dataset ds )
	{
		int channel = ds.dimensionIndex( Axes.CHANNEL );
		return ( channel == ds.numDimensions() - 1 ) ? ds :
				Views.stack( IntStream.of(0,1,2).mapToObj( i -> Views.hyperSlice( ds, channel, i ) ).collect( Collectors.toList() ) );
	}

	private void convertToColor( Composite<RealType<?>> in, ARGBType out )
	{
		out.set( ARGBType.rgba( toByte( in.get( 0 ) ), toByte( in.get( 1 ) ), toByte( in.get( 2 ) ), 255) );
	}

	private int toByte( RealType<?> realType )
	{
		return (int) realType.getRealFloat();
	}

}
