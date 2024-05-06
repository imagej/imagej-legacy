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

package net.imagej.legacy.convert.roi.box;

import ij.gui.Roi;

import net.imagej.legacy.convert.roi.IJRealRoiWrapper;
import net.imagej.legacy.convert.roi.Rois;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.WritableBox;
import net.imglib2.roi.util.AbstractRealMaskPoint;
import net.imglib2.roi.util.RealLocalizableRealPositionable;

/**
 * Wraps an ImageJ 1.x {@link Roi} as an ImgLib2 {@link Box}. Even though all
 * ImageJ 1.x ROIs are {@link Roi Rois}, this class should only be used to wrap
 * ROIs intended to be rectangles.
 *
 * @author Alison Walter
 */
public class RoiWrapper implements IJRealRoiWrapper<Roi>, WritableBox {

	private final Roi rect;

	/**
	 * Creates an ImageJ 1.x {@link Roi} and then wraps it as an ImgLib2
	 * {@link Box}.
	 *
	 * @param x x coordinate of upper left corner
	 * @param y y coordinate of upper left corner
	 * @param width width of rectangle
	 * @param height height of rectangle
	 */
	public RoiWrapper(final double x, final double y, final double width,
		final double height)
	{
		rect = new Roi(x, y, width, height);
	}

	/**
	 * Wraps an ImageJ 1.x {@link Roi} as an ImgLib2 {@link Box}.
	 *
	 * @param roi the {@link Roi} to be wrapped
	 */
	public RoiWrapper(final Roi roi) {
		if (roi.getCornerDiameter() != 0) throw new IllegalArgumentException(
			"Cannot wrap Roi with rounded corners");
		rect = roi;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * In an attempt to be consistent with ImageJ 1.x
	 * {@link Roi#contains(int, int)}, only some of the edges are considered
	 * "contained" by this rectangle. Therefore, this Box has
	 * {@link net.imglib2.roi.BoundaryType#UNSPECIFIED unspecified} boundary
	 * behavior.
	 * </p>
	 */
	@Override
	public boolean test(final RealLocalizable t) {
		final double x = t.getDoublePosition(0);
		final double y = t.getDoublePosition(1);

		return x >= realMin(0) && x < realMax(0) && y >= realMin(1) && y < realMax(
			1);
	}

	@Override
	public double realMin(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? rect.getXBase() : rect.getYBase();
	}

	@Override
	public double realMax(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? rect.getXBase() + rect.getFloatWidth() : rect.getYBase() +
			rect.getFloatHeight();
	}

	@Override
	public double sideLength(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? rect.getFloatWidth() : rect.getFloatHeight();
	}

	@Override
	public RealLocalizableRealPositionable center() {
		return new BoxCenter(rect.getXBase() + rect.getFloatWidth() / 2.0, rect
			.getYBase() + rect.getFloatHeight() / 2.0);
	}

	/**
	 * This will <strong>always</strong> throw an
	 * {@code UnsupportedOperationException}.
	 *
	 * @throws UnsupportedOperationException cannot modify width/height of
	 *           underlying {@link Roi}
	 */
	@Override
	public void setSideLength(final int d, final double length) {
		Rois.unsupported("setSideLength");
	}

	@Override
	public Roi getRoi() {
		return rect;
	}

	@Override
	public int hashCode() {
		return Box.hashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Box && Box.equals(this, (Box) obj);
	}

	// -- Helper classes --

	private class BoxCenter extends AbstractRealMaskPoint {

		public BoxCenter(final double x, final double y) {
			super(new double[] { x, y });
		}

		@Override
		public void updateBounds() {
			// Update ImageJ 1.x Roi location
			final double x = position[0] - rect.getFloatWidth() / 2;
			final double y = position[1] - rect.getFloatHeight() / 2;

			rect.setLocation(x, y);
		}

	}

}
