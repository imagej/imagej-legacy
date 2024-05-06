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

package net.imagej.legacy.convert.roi.polygon2d;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import net.imagej.legacy.convert.roi.AbstractPolygonRoiWrapper;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.GeomMaths;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.Polyshape;

import gnu.trove.list.array.TDoubleArrayList;

/**
 * Wraps an ImageJ 1.x {@link PolygonRoi} of type {@link Roi#FREEROI} or
 * {@link Roi#TRACED_ROI} as an unmodifiable ImgLib2 {@link Polygon2D}.
 *
 * @author Alison Walter
 */
public class UnmodifiablePolygonRoiWrapper extends AbstractPolygonRoiWrapper
	implements Polygon2D
{

	/**
	 * Wraps an ImageJ 1.x {@link PolygonRoi} as an ImgLib2 {@link Polygon2D}.
	 *
	 * @param poly the {@code PolygonRoi} to be wrapped
	 */
	public UnmodifiablePolygonRoiWrapper(final PolygonRoi poly) {
		super(poly);
		if (poly.getType() != Roi.FREEROI && poly.getType() != Roi.TRACED_ROI)
			throw new IllegalArgumentException("Cannot wrap " + poly
				.getTypeAsString() + " as Polygon2D");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Since {@link PolygonRoi#contains(int, int)} uses the "pnpoly" algorithm for
	 * real space polygons, so does this implementation. Thus resulting in a
	 * {@code Polygon2D} with {@link net.imglib2.roi.BoundaryType#UNSPECIFIED
	 * unspecified} boundary behavior.
	 * </p>
	 */
	@Override
	public boolean test(final RealLocalizable t) {
		final float[] xf = getRoi().getFloatPolygon().xpoints;
		final float[] yf = getRoi().getFloatPolygon().ypoints;
		final TDoubleArrayList x = new TDoubleArrayList(getRoi().getNCoordinates());
		final TDoubleArrayList y = new TDoubleArrayList(getRoi().getNCoordinates());

		for (int i = 0; i < getRoi().getNCoordinates(); i++) {
			x.add(xf[i]);
			y.add(yf[i]);
		}

		return GeomMaths.pnpoly(x, y, t);
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
		return Polygon2D.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Polygon2D && Polyshape.equals(this, (Polygon2D) obj);
	}

}
