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

import net.imagej.legacy.convert.roi.IJRealRoiWrapper;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.RealMaskRealInterval;

/**
 * Wraps a {@link PolygonRoi} of type {@link Roi#POLYLINE} or
 * {@link Roi#FREELINE} with a non zero width as a {@link RealMaskRealInterval}.
 * This can also be used to wrap {@link Roi#POLYLINE} which have been spline
 * fitted.
 * <p>
 * This is only intended to wrap existing ImageJ 1.x Rois, not to create ImgLib2
 * Rois backed by ImageJ 1.x Rois.
 * </p>
 *
 * @author Alison Walter
 */
public class IrregularPolylineRoiWrapper implements
	IJRealRoiWrapper<PolygonRoi>
{

	private final PolygonRoi roi;

	/**
	 * Creates a {@link RealMaskRealInterval} which wraps the given {@link Roi}.
	 *
	 * @param roi the Roi to be wrapped
	 */
	public IrregularPolylineRoiWrapper(final PolygonRoi roi) {
		this.roi = roi;
		if (!roi.isLine()) throw new IllegalArgumentException("Cannot wrap " + roi
			.getTypeAsString() + " as Polyline with non-zero width");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This includes some additional points near the vertices of the polyline.
	 * </p>
	 */
	@Override
	public boolean test(final RealLocalizable t) {
		final float[] x = roi.getFloatPolygon().xpoints;
		final float[] y = roi.getFloatPolygon().ypoints;

		final double xt = t.getDoublePosition(0);
		final double yt = t.getDoublePosition(1);
		for (int i = 1; i < getRoi().getNCoordinates(); i++) {
			if (lineContains(x[i - 1], y[i - 1], x[i], y[i], xt, yt, roi
				.getStrokeWidth())) return true;
		}

		return false;
	}

	@Override
	public BoundaryType boundaryType() {
		return BoundaryType.CLOSED;
	}

	@Override
	public PolygonRoi getRoi() {
		return roi;
	}

	@Override
	public double realMin(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		// Bounds of the underlying roi are not updated when the width changes, so
		// in order to ensure this occurs it needs to be subtracted. This may result
		// in a bounding box which is slightly larger than needed.
		return d == 0 ? roi.getXBase() - roi.getStrokeWidth() / 2 : roi.getYBase() -
			roi.getStrokeWidth() / 2;
	}

	@Override
	public double realMax(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		// Bounds of the underlying roi are not updated when the width changes, so
		// in order to ensure this occurs it needs to be added. This may result in a
		// bounding box which is slightly larger than needed.
		return d == 0 ? roi.getXBase() + roi.getFloatWidth() + roi
			.getStrokeWidth() / 2 : roi.getYBase() + roi.getFloatHeight() + roi
				.getStrokeWidth() / 2;
	}

	// -- Helper methods --

	private boolean lineContains(final double x1, final double y1,
		final double x2, final double y2, final double xt, final double yt,
		final double width)
	{
		final double[] directionVector = new double[] { x2 - x1, y2 - y1 };
		final double magnitude = Math.sqrt((directionVector[0] *
			directionVector[0]) + (directionVector[1] * directionVector[1]));
		directionVector[0] = directionVector[0] / magnitude;
		directionVector[1] = directionVector[1] / magnitude;

		final double projection = (xt - x1) * directionVector[0] + (yt - y1) *
			directionVector[1];

		double xp = x1 + (projection * directionVector[0]);
		double yp = y1 + (projection * directionVector[1]);

		if (xp > Math.max(x1, x2)) xp = Math.max(x1, x2);
		if (xp < Math.min(x1, x2)) xp = Math.min(x1, x2);
		if (yp > Math.max(y1, y2)) yp = Math.max(y1, y2);
		if (yp < Math.min(y1, y2)) yp = Math.min(y1, y2);

		final boolean result;

		if (width == 0) result = ((xp - xt) * (xp - xt) + (yp - yt) * (yp -
			yt)) <= 1e-15;
		else result = Math.sqrt(((xp - xt) * (xp - xt) + (yp - yt) * (yp -
			yt))) <= width / 2;
		return result;
	}
}
