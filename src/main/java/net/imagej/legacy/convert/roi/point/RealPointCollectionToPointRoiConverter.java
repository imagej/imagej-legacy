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

import java.util.Iterator;

import net.imagej.legacy.convert.roi.AbstractMaskPredicateToRoiConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.RealPointCollection;

import org.scijava.Priority;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Converts a {@link RealPointCollection} to a {@link PointRoi}. This conversion
 * may be lossy since PointRoi coordinates are stored as {@code float}s.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.LOW)
public class RealPointCollectionToPointRoiConverter extends
	AbstractMaskPredicateToRoiConverter<RealPointCollection<RealLocalizable>, PointRoi>
{

	@Override
	public Class<PointRoi> getOutputType() {
		return PointRoi.class;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<RealPointCollection<RealLocalizable>> getInputType() {
		return (Class) RealPointCollection.class;
	}

	@Override
	public PointRoi convert(final RealPointCollection<RealLocalizable> mask) {
		final Iterator<RealLocalizable> points = mask.points().iterator();
		RealLocalizable point = points.next();
		final PointRoi pointRoi = new PointRoi(point.getDoublePosition(0), point
			.getDoublePosition(1));

		while (points.hasNext()) {
			point = points.next();
			pointRoi.addPoint(point.getDoublePosition(0), point.getDoublePosition(1));
		}

		return pointRoi;
	}

	@Override
	public boolean isLossy() {
		return true;
	}
}
