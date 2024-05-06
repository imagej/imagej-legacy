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

package net.imagej.legacy.convert.roi.ellipsoid;

import ij.gui.OvalRoi;

import net.imagej.legacy.convert.roi.IJRealRoiWrapper;
import net.imagej.legacy.convert.roi.Rois;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.BoundaryType;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.SuperEllipsoid;
import net.imglib2.roi.geom.real.WritableEllipsoid;
import net.imglib2.roi.util.AbstractRealMaskPoint;
import net.imglib2.roi.util.RealLocalizableRealPositionable;
import net.imglib2.util.Intervals;

/**
 * Wraps an ImageJ 1.x {@link OvalRoi} as an ImgLib2 {@link Ellipsoid}.
 *
 * @author Alison Walter
 */
public class OvalRoiWrapper implements IJRealRoiWrapper<OvalRoi>,
	WritableEllipsoid
{

	private final OvalRoi oval;

	/**
	 * Creates an ImageJ 1.x {@link OvalRoi} and then wraps it as an ImgLib2
	 * {@link Ellipsoid}.
	 *
	 * @param x x coordinate of the upper left corner of the bounding box, i.e. (x
	 *          coor of center) - (x semi-axis length)
	 * @param y y coordinate of the upper left corner of the bounding box, i.e. (y
	 *          coor of center) - (y semi-axis length)
	 * @param width width of the bounding box, i.e. 2 * (x semi-axis length)
	 * @param height height of the bounding box, i.e. 2 * (y semi-axis length)
	 */
	public OvalRoiWrapper(final int x, final int y, final int width,
		final int height)
	{
		oval = new OvalRoi(x, y, width, height);
	}

	/**
	 * Creates an ImageJ 1.x {@link OvalRoi} and then wraps it as an ImgLib2
	 * {@link Ellipsoid}.
	 *
	 * @param x x coordinate of the upper left corner of the bounding box, i.e. (x
	 *          coor of center) - (x semi-axis length)
	 * @param y y coordinate of the upper left corner of the bounding box, i.e. (y
	 *          coor of center) - (y semi-axis length)
	 * @param width width of the bounding box, i.e. 2 * (x semi-axis length)
	 * @param height height of the bounding box, i.e. 2 * (y semi-axis length)
	 */
	public OvalRoiWrapper(final double x, final double y, final double width,
		final double height)
	{
		oval = new OvalRoi(x, y, width, height);
	}

	/**
	 * Wraps the given ImageJ 1.x {@link OvalRoi} as an ImgLib2 {@link Ellipsoid}.
	 *
	 * @param oval ImageJ 1.x oval to be wrapped
	 */
	public OvalRoiWrapper(final OvalRoi oval) {
		this.oval = oval;
	}

	@Override
	public boolean test(final RealLocalizable t) {
		// NB: ImageJ 1.x contains(...) is not used due to the limitations of
		// integer coordinates. ImageJ 1.x contains method does use `<= 1` which
		// results in a closed ellipsoid.
		if (Intervals.contains(this, t)) {
			final double xr = semiAxisLength(0);
			final double yr = semiAxisLength(1);
			final RealLocalizable c = center();
			final double xt = t.getDoublePosition(0);
			final double yt = t.getDoublePosition(1);

			return (((xt - c.getDoublePosition(0)) / xr) * ((xt - c.getDoublePosition(
				0)) / xr)) + (((yt - c.getDoublePosition(1)) / yr) * ((yt - c
					.getDoublePosition(1)) / yr)) <= 1.0;
		}
		return false;
	}

	@Override
	public double realMin(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? oval.getXBase() : oval.getYBase();
	}

	@Override
	public double realMax(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? oval.getXBase() + oval.getFloatWidth() : oval.getYBase() +
			oval.getFloatHeight();
	}

	@Override
	public double exponent() {
		return 2;
	}

	@Override
	public double semiAxisLength(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? oval.getFloatWidth() / 2 : oval.getFloatHeight() / 2;
	}

	@Override
	public RealLocalizableRealPositionable center() {
		return new OvalCenter((realMin(0) + realMax(0)) / 2.0, (realMin(1) +
			realMax(1)) / 2.0);
	}

	/**
	 * This will <strong>always</strong> throw an
	 * {@code UnsupportedOperationException}.
	 *
	 * @throws UnsupportedOperationException cannot modify width/height of
	 *           underlying {@link OvalRoi}
	 */
	@Override
	public void setSemiAxisLength(final int d, final double length) {
		Rois.unsupported("setSemiAxisLength");
	}

	@Override
	public BoundaryType boundaryType() {
		// The contains(...) in OvalRoi is closed, so the wrapper will be as
		// well
		return BoundaryType.CLOSED;
	}

	@Override
	public OvalRoi getRoi() {
		return oval;
	}

	@Override
	public int hashCode() {
		return SuperEllipsoid.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof SuperEllipsoid && SuperEllipsoid.equals(this, (SuperEllipsoid) obj);
	}

	// -- Helper classes --

	private class OvalCenter extends AbstractRealMaskPoint {

		public OvalCenter(final double x, final double y) {
			super(new double[] { x, y });
		}

		@Override
		public void updateBounds() {
			// Updates ImageJ 1.x OvalRoi position
			final double x = position[0] - oval.getFloatWidth() / 2;
			final double y = position[1] - oval.getFloatHeight() / 2;
			oval.setLocation(x, y);
		}

	}
}
