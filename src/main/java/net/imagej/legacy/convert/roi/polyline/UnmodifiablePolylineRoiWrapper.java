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

package net.imagej.legacy.convert.roi.polyline;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import net.imagej.legacy.convert.roi.AbstractPolygonRoiWrapper;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.GeomMaths;
import net.imglib2.roi.geom.real.Polyline;
import net.imglib2.roi.geom.real.Polyshape;
import net.imglib2.util.Intervals;

/**
 * Wraps an ImageJ 1.x {@link PolygonRoi} of type {@link Roi#FREELINE} and
 * {@link Roi#ANGLE} as an unmodifiable ImgLib2 {@link Polyline}.
 *
 * @author Alison Walter
 */
public class UnmodifiablePolylineRoiWrapper extends AbstractPolygonRoiWrapper
	implements Polyline
{

	/**
	 * Wraps an ImageJ 1.x {@link PolygonRoi} as an ImgLib2 {@link Polyline}.
	 *
	 * @param poly the {@code PolygonRoi} to be wrapped
	 */
	public UnmodifiablePolylineRoiWrapper(final PolygonRoi poly) {
		super(poly);
		if (poly.getType() != Roi.FREELINE && poly.getType() != Roi.ANGLE)
			throw new IllegalArgumentException("Cannot wrap " + poly
				.getTypeAsString() + " as Polyline");
		if (poly.getStrokeWidth() != 0) throw new IllegalArgumentException(
			"Cannot wrap polylines with non-zero width");
		if (poly.isSplineFit()) throw new IllegalArgumentException("Cannot wrap " +
			"spline fitted polylines");
	}

	@Override
	public boolean test(final RealLocalizable t) {
		if (Intervals.contains(this, t)) {
			final float[] x = getRoi().getFloatPolygon().xpoints;
			final float[] y = getRoi().getFloatPolygon().ypoints;

			for (int i = 1; i < numVertices(); i++) {
				final double[] start = new double[] { x[i - 1], y[i - 1] };
				final double[] end = new double[] { x[i], y[i] };
				final boolean testLineContains = GeomMaths.lineContains(start, end, t,
					2);
				if (testLineContains) return true;
			}
		}
		return false;
	}

	@Override
	public RealLocalizable vertex(final int pos) {
		final FloatPolygon fp = getRoi().getFloatPolygon();
		return new AbstractRealLocalizable(new double[] { fp.xpoints[pos],
			fp.ypoints[pos] })
		{};
	}

	@Override
	public int hashCode() {
		return Polyline.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Polyline && Polyshape.equals(this, (Polyline) obj);
	}

}
