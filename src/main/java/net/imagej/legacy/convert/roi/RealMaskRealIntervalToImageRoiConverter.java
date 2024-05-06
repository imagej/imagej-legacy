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

package net.imagej.legacy.convert.roi;

import ij.ImagePlus;
import ij.gui.ImageRoi;

import java.lang.reflect.Type;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessibleRealInterval;
import net.imglib2.roi.Masks;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.type.logic.BoolType;
import net.imglib2.view.Views;

import org.scijava.Priority;
import org.scijava.convert.ConversionRequest;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Converts a {@link RealMaskRealInterval} to an {@link ImageRoi}. This
 * conversion is lossy, since the MaskRealInterval must be rasterized.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.LOW)
public class RealMaskRealIntervalToImageRoiConverter extends
	AbstractMaskPredicateToRoiConverter<RealMaskRealInterval, ImageRoi>
{

	@Parameter
	private ConvertService convertService;

	@Parameter(required = false)
	private DatasetService datasetService;

	@Override
	public boolean canConvert(final ConversionRequest request) {
		return datasetService != null && super.canConvert(request);
	}

	@Override
	public boolean canConvert(final Object src, final Type dest) {
		return datasetService != null && super.canConvert(src, dest);
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		return datasetService != null && super.canConvert(src, dest);
	}

	@Override
	public boolean canConvert(final Class<?> src, final Class<?> dest) {
		return datasetService != null && super.canConvert(src, dest);
	}

	@Override
	public Class<ImageRoi> getOutputType() {
		return ImageRoi.class;
	}

	@Override
	public Class<RealMaskRealInterval> getInputType() {
		return RealMaskRealInterval.class;
	}

	@Override
	public ImageRoi convert(final RealMaskRealInterval mask) {
		// Wrap mask as RRARI
		final RealRandomAccessibleRealInterval<BoolType> rrari = Masks
			.toRealRandomAccessibleRealInterval(mask);

		// Convert the RRARI to a RAI whose min is (0, 0), this will ensure it
		// displays properly
		final RandomAccessible<BoolType> raster = Views.raster(rrari);
		final RandomAccessible<BoolType> translate = Views.translate(raster,
			new long[] { (long) -mask.realMin(0), (long) -mask.realMin(1) });
		final RandomAccessibleInterval<BoolType> rai = Views.interval(translate,
			new long[] { 0, 0 }, new long[] { (long) (mask.realMax(0) - mask.realMin(
				0)), (long) (mask.realMax(1) - mask.realMin(1)) });

		// Convert RAI to ImagePlus
		final Dataset d = datasetService.create(rai);
		final ImagePlus ip = convertService.convert(d, ImagePlus.class);

		return new ImageRoi((int) mask.realMin(0), (int) mask.realMin(1), ip
			.getBufferedImage());
	}

	@Override
	public boolean isLossy() {
		return true;
	}
}
