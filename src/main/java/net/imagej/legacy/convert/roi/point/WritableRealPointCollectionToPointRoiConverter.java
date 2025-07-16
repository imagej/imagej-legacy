/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2025 ImageJ2 developers.
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

package net.imagej.legacy.convert.roi.point;

import ij.gui.PointRoi;

import net.imagej.legacy.convert.roi.AbstractMaskPredicateToRoiConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.roi.geom.real.WritableRealPointCollection;

import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * Converts a {@link WritableRealPointCollection} to a {@link PointRoi}. This
 * conversion may be lossy since PointRoi coordinates are stored as
 * {@code float}s.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class WritableRealPointCollectionToPointRoiConverter<L extends RealLocalizable & RealPositionable>
	extends
	AbstractMaskPredicateToRoiConverter<WritableRealPointCollection<L>, PointRoi>
{

	@Parameter
	public ConvertService convert;

	@Override
	public Class<PointRoi> getOutputType() {
		return PointRoi.class;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<WritableRealPointCollection<L>> getInputType() {
		return (Class) WritableRealPointCollection.class;
	}

	@Override
	public PointRoi convert(final WritableRealPointCollection<L> mask) {
		float[] srcArray = { 0f, 0f };
		Class<?> ptClass = mask.points().iterator().next().getClass();
		Converter<float[], L> c = (Converter<float[], L>) convert.getHandler(
			srcArray, ptClass);
		Supplier<L> ptCreator = () -> (L) c.convert(srcArray, ptClass);
		return new RealPointCollectionWrapper<>(mask, ptCreator);
	}

	@Override
	public boolean isLossy() {
		return true;
	}
}
