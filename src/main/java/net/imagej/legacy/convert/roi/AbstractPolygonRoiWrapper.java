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

import ij.gui.PolygonRoi;
import ij.gui.Roi;

import net.imglib2.roi.Mask;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.Polyline;

/**
 * Abstract base class for wrapping ImageJ 1.x objects which are
 * {@link PolygonRoi}s (i.e. POLYGON, POLYLINE, FREELINE, etc.).
 *
 * @author Alison Walter
 */
public abstract class AbstractPolygonRoiWrapper implements
	IJRealRoiWrapper<PolygonRoi>
{

	private final PolygonRoi poly;

	/**
	 * Creates an ImageJ 1.x {@link PolygonRoi} and wraps it as an ImgLib2
	 * {@link Polygon2D} or {@link Polyline}.
	 *
	 * @param xPoints x coordinates of the vertices
	 * @param yPoints y coordinates of the vertices
	 * @param nPoints number of vertices
	 * @param type denotes if PolygonRoi behaves as {@link Roi#POLYGON} or
	 *          {@link Roi#POLYLINE}
	 */
	public AbstractPolygonRoiWrapper(final int[] xPoints, final int[] yPoints,
		final int nPoints, final int type)
	{
		poly = new PolygonRoi(xPoints, yPoints, nPoints, type);
	}

	/**
	 * Creates an ImageJ 1.x {@link PolygonRoi} and wraps it as an ImgLib2
	 * {@link Polygon2D} or {@link Polyline}.
	 *
	 * @param xPoints x coordinates of the vertices
	 * @param yPoints y coordinates of the vertices
	 * @param nPoints number of vertices
	 * @param type denotes if PolygonRoi behaves as {@link Roi#POLYGON} or
	 *          {@link Roi#POLYLINE}
	 */
	public AbstractPolygonRoiWrapper(final float[] xPoints, final float[] yPoints,
		final int nPoints, final int type)
	{
		poly = new PolygonRoi(xPoints, yPoints, nPoints, type);
	}

	/**
	 * Creates an ImageJ 1.x {@link PolygonRoi} and wraps it as an ImgLib2
	 * {@link Mask}. The length of {@code xPoints} will be used to determine the
	 * number of vertices this {@code Mask} has.
	 *
	 * @param xPoints x coordinates of the vertices
	 * @param yPoints y coordinates of the vertices
	 * @param type denotes if PolygonRoi behaves as {@link Roi#POLYGON} or
	 *          {@link Roi#POLYLINE}
	 */
	public AbstractPolygonRoiWrapper(final float[] xPoints, final float[] yPoints,
		final int type)
	{
		poly = new PolygonRoi(xPoints, yPoints, type);
	}

	/**
	 * Wraps an ImageJ 1.x {@link PolygonRoi} as an ImgLib2 {@link Mask}.
	 *
	 * @param poly the {@code PolygonRoi} to be wrapped
	 */
	public AbstractPolygonRoiWrapper(final PolygonRoi poly) {
		if (poly.isSplineFit()) throw new IllegalArgumentException(
			"Cannot wrap spline fit PolygonRois");
		this.poly = poly;
	}

	@Override
	public double realMin(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		// NB: bounding box doesn't update after vertex removed
		final float[] c = d == 0 ? poly.getFloatPolygon().xpoints : poly
			.getFloatPolygon().ypoints;
		double min = Double.POSITIVE_INFINITY;
		for (int i = 0; i < numVertices(); i++)
			if (c[i] < min) min = c[i];
		return min;
	}

	@Override
	public double realMax(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		// NB: bounding box doesn't update after vertex removed
		final float[] c = d == 0 ? poly.getFloatPolygon().xpoints : poly
			.getFloatPolygon().ypoints;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < numVertices(); i++)
			if (c[i] > max) max = c[i];
		return max;
	}

	public int numVertices() {
		return poly.getNCoordinates();
	}

	@Override
	public PolygonRoi getRoi() {
		return poly;
	}

	@Override
	public int numDimensions() {
		return 2;
	}

}
