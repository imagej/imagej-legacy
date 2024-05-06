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

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.util.Collection;

import net.imagej.legacy.convert.roi.AbstractPolygonRoiWrapper;
import net.imagej.legacy.convert.roi.Rois;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.GeomMaths;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.Polyshape;
import net.imglib2.roi.geom.real.WritablePolygon2D;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

import gnu.trove.list.array.TDoubleArrayList;

/**
 * Wraps an ImageJ 1.x {@link PolygonRoi} as an ImgLib2 {@link Polygon2D}.
 *
 * @author Alison Walter
 */
public class PolygonRoiWrapper extends AbstractPolygonRoiWrapper implements
	WritablePolygon2D
{

	/**
	 * Creates an ImageJ 1.x {@link PolygonRoi} and wraps it as an ImgLib2
	 * {@link Polygon2D}.
	 *
	 * @param xPoints x coordinates of the vertices
	 * @param yPoints y coordinates of the vertices
	 * @param nPoints number of vertices
	 */
	public PolygonRoiWrapper(final int[] xPoints, final int[] yPoints,
		final int nPoints)
	{
		super(xPoints, yPoints, nPoints, Roi.POLYGON);
	}

	/**
	 * Creates an ImageJ 1.x {@link PolygonRoi} and wraps it as an ImgLib2
	 * {@link Polygon2D}.
	 *
	 * @param xPoints x coordinates of the vertices
	 * @param yPoints y coordinates of the vertices
	 * @param nPoints number of vertices
	 */
	public PolygonRoiWrapper(final float[] xPoints, final float[] yPoints,
		final int nPoints)
	{
		super(xPoints, yPoints, nPoints, Roi.POLYGON);
	}

	/**
	 * Creates an ImageJ 1.x {@link PolygonRoi} and wraps it as an ImgLib2
	 * {@link Polygon2D}. The length of {@code xPoints} will be used to determine
	 * the number of vertices this {@code Polygon2D} has.
	 *
	 * @param xPoints x coordinates of the vertices
	 * @param yPoints y coordinates of the vertices
	 */
	public PolygonRoiWrapper(final float[] xPoints, final float[] yPoints) {
		super(xPoints, yPoints, Roi.POLYGON);
	}

	/**
	 * Wraps an ImageJ 1.x {@link PolygonRoi} as an ImgLib2 {@link Polygon2D}.
	 *
	 * @param poly the {@code PolygonRoi} to be wrapped
	 */
	public PolygonRoiWrapper(final PolygonRoi poly) {
		super(poly);
		if (poly.getType() != Roi.POLYGON) throw new IllegalArgumentException(
			"Cannot wrap " + poly.getTypeAsString() + " as Polygon2D");
		if (poly.isSplineFit()) throw new IllegalArgumentException("Cannot wrap " +
			"spline fitted polygons");
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
	public RealLocalizableRealPositionable vertex(final int pos) {
		final FloatPolygon fp = getRoi().getFloatPolygon();
		return Rois.ijRoiPoint(fp.xpoints[pos], fp.ypoints[pos]);
	}

	/**
	 * This will <strong>always</strong> throw an
	 * {@code UnsupportedOperationException}.
	 *
	 * @throws UnsupportedOperationException cannot add a new vertex to the
	 *           underlying {@link PolygonRoi}
	 */
	@Override
	public void addVertex(final int index, final RealLocalizable vertex) {
		Rois.unsupported("addVertex");
	}

	/**
	 * This will <strong>always</strong> throw an
	 * {@code UnsupportedOperationException}.
	 *
	 * @throws UnsupportedOperationException cannot add new vertices to the
	 *           underlying {@link PolygonRoi}
	 */
	@Override
	public void addVertices(final int index,
		final Collection<RealLocalizable> vertices)
	{
		Rois.unsupported("addVertices");
	}

	/**
	 * If the wrapped {@link PolygonRoi} is not associated with an
	 * {@link ImagePlus}, then this method will always throw an
	 * {@code UnsupportedOperationException}. Otherwise, the vertex will be
	 * removed provided the index is valid.
	 */
	@Override
	public void removeVertex(final int index) {
		if (getRoi().getImage() != null) {
			final double x = getRoi().getFloatPolygon().xpoints[index];
			final double y = getRoi().getFloatPolygon().ypoints[index];
			getRoi().deleteHandle(x, y);
		}
		else Rois.unsupported("removeVertex");
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
