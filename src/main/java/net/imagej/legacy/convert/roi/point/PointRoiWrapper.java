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

import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.process.FloatPolygon;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import net.imagej.legacy.convert.roi.IJRealRoiWrapper;
import net.imagej.legacy.convert.roi.Rois;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.RealPointCollection;
import net.imglib2.roi.geom.real.WritableRealPointCollection;

/**
 * Wraps an ImageJ 1.x {@link PointRoi} as an ImgLib2
 * {@link RealPointCollection}.
 *
 * @author Alison Walter
 */
public class PointRoiWrapper implements IJRealRoiWrapper<PointRoi>,
	WritableRealPointCollection<RealLocalizable>
{

	private final PointRoi points;

	/**
	 * Creates an ImageJ 1.x {@link PointRoi} and wraps it as an ImgLib2
	 * {@link RealPointCollection}.
	 *
	 * @param x x coordinates of the points
	 * @param y y coordinates of the points
	 * @param numPoints total number of points in the collection
	 */
	public PointRoiWrapper(final int[] x, final int[] y, final int numPoints) {
		this.points = new PointRoi(x, y, numPoints);
	}

	/**
	 * Creates an ImageJ 1.x {@link PointRoi} and wraps it as an ImgLib2
	 * {@link RealPointCollection}.
	 *
	 * @param x x coordinates of the points
	 * @param y y coordinates of the points
	 * @param numPoints total number of points in the collection
	 */
	public PointRoiWrapper(final float[] x, final float[] y,
		final int numPoints)
	{
		this.points = new PointRoi(x, y, numPoints);
	}

	/**
	 * Creates an ImageJ 1.x {@link PointRoi} and wraps it as an ImgLib2
	 * {@link RealPointCollection}.
	 *
	 * @param x x coordinates of the points
	 * @param y y coordinates of the points
	 */
	public PointRoiWrapper(final float[] x, final float[] y) {
		points = new PointRoi(x, y);
	}

	/**
	 * Creates an ImageJ 1.x {@link PointRoi} and wraps it as an ImgLib2
	 * {@link RealPointCollection}.
	 *
	 * @param polygon {@link FloatPolygon} whose vertices will be the points
	 *          contained in this collection
	 */
	public PointRoiWrapper(final FloatPolygon polygon) {
		points = new PointRoi(polygon);
	}

	/**
	 * Creates an ImageJ 1.x {@link PointRoi} and wraps it as an ImgLib2
	 * {@link RealPointCollection}.
	 *
	 * @param polygon {@link Polygon} whose vertices will be the points contained
	 *          in this collection
	 */
	public PointRoiWrapper(final Polygon polygon) {
		points = new PointRoi(polygon);
	}

	/**
	 * Creates an ImageJ 1.x {@link PointRoi} and wraps it as an ImgLib2
	 * {@link RealPointCollection}.
	 *
	 * @param x x coordinate of the point
	 * @param y y coordinate of the point
	 */
	public PointRoiWrapper(final int x, final int y) {
		points = new PointRoi(x, y);
	}

	/**
	 * Creates an ImageJ 1.x {@link PointRoi} and wraps it as an ImgLib2
	 * {@link RealPointCollection}.
	 *
	 * @param x x coordinate of the point
	 * @param y y coordinate of the point
	 */
	public PointRoiWrapper(final double x, final double y) {
		points = new PointRoi(x, y);
	}

	/**
	 * Wraps the given ImageJ 1.x {@link PointRoi} as an ImgLib2
	 * {@link RealPointCollection}.
	 *
	 * @param points {@link PointRoi} to be wrapped
	 */
	public PointRoiWrapper(final PointRoi points) {
		this.points = points;
	}

	@Override
	public boolean test(final RealLocalizable t) {
		// NB: ImageJ 1.x contains(...) is not used due to the limitations of
		// integer coordinates.
		final float xt = t.getFloatPosition(0);
		final float yt = t.getFloatPosition(1);
		final float[] x = points.getContainedFloatPoints().xpoints;
		final float[] y = points.getContainedFloatPoints().ypoints;
		final int numPoints = points.getNCoordinates();

		for (int i = 0; i < numPoints; i++)
			if (xt == x[i] && yt == y[i]) return true;
		return false;
	}

	@Override
	public double realMin(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);

		final int numPoints = points.getNCoordinates();
		double min = Double.POSITIVE_INFINITY;

		if (d == 0) {
			final float[] x = points.getContainedFloatPoints().xpoints;
			for (int i = 0; i < numPoints; i++)
				if (x[i] < min) min = x[i];
			return min;
		}

		final float[] y = points.getContainedFloatPoints().ypoints;
		for (int i = 0; i < numPoints; i++)
			if (y[i] < min) min = y[i];
		return min;
	}

	@Override
	public double realMax(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);

		final int numPoints = points.getNCoordinates();
		double max = Double.NEGATIVE_INFINITY;

		if (d == 0) {
			final float[] x = points.getContainedFloatPoints().xpoints;
			for (int i = 0; i < numPoints; i++)
				if (x[i] > max) max = x[i];
			return max;
		}

		final float[] y = points.getContainedFloatPoints().ypoints;
		for (int i = 0; i < numPoints; i++)
			if (y[i] > max) max = y[i];
		return max;
	}

	@Override
	public Iterable<RealLocalizable> points() {
		final List<RealLocalizable> pts = new ArrayList<>();
		final float[] x = points.getContainedFloatPoints().xpoints;
		final float[] y = points.getContainedFloatPoints().ypoints;
		final int numPoints = points.getNCoordinates();

		for (int i = 0; i < numPoints; i++) {
			pts.add(new AbstractRealLocalizable(new double[] { x[i], y[i] }) {});
		}
		return pts;
	}

	@Override
	public long size() {
		return points.getNCoordinates();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The position of this point will be stored as a {@code float}, which may
	 * result in a loss of precision.
	 * </p>
	 */
	@Override
	public void addPoint(final RealLocalizable point) {
		points.addPoint(point.getDoublePosition(0), point.getDoublePosition(1));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If there is no {@link ImagePlus} associated with the wrapped
	 * {@link PointRoi}, then an {@code UnsupportedOperationException} will always
	 * be thrown. Otherwise, the point will be removed if it matches any of the
	 * points in the collection.
	 * </p>
	 */
	@Override
	public void removePoint(final RealLocalizable point) {
		if (points.getImage() != null) {
			// NB: deleteHandle will remove the point nearest to the given point
			// if an
			// exact match is not found. So test that the point is part of the
			// roi
			// first.
			if (test(point)) points.deleteHandle(point.getDoublePosition(0), point
				.getDoublePosition(1));
		}
		else Rois.unsupported("removePoint");
	}

	@Override
	public PointRoi getRoi() {
		return points;
	}

	@Override
	public int hashCode() {
		return RealPointCollection.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof RealPointCollection && //
			RealPointCollection.equals(this, (RealPointCollection<?>) obj);
	}

}
