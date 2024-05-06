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

package net.imagej.legacy.convert.roi.point;

import ij.gui.PointRoi;
import ij.gui.Roi;

import net.imagej.legacy.convert.roi.AbstractRoiToMaskPredicateConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.RealPointCollection;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Converts an ImageJ 1.x {@link PointRoi} to an ImgLib2
 * {@link RealPointCollection}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class PointRoiToRealPointCollectionConverter extends
	AbstractRoiToMaskPredicateConverter<PointRoi, RealPointCollection<RealLocalizable>>
{

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<RealPointCollection<RealLocalizable>> getOutputType() {
		return (Class) RealPointCollection.class;
	}

	@Override
	public Class<PointRoi> getInputType() {
		return PointRoi.class;
	}

	@Override
	public RealPointCollection<RealLocalizable> convert(final PointRoi src) {
		return new PointRoiWrapper(src);
	}

	@Override
	public boolean supportedType(final PointRoi src) {
		return src.getType() == Roi.POINT;
	}

}
