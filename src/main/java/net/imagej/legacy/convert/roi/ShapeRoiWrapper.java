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

import ij.gui.ShapeRoi;

import net.imglib2.RealLocalizable;
import net.imglib2.roi.RealMaskRealInterval;

/**
 * Wraps an ImageJ 1.x {@link ShapeRoi} as an ImgLib2
 * {@link RealMaskRealInterval}.
 *
 * @author Alison Walter
 */
public class ShapeRoiWrapper implements IJRealRoiWrapper<ShapeRoi> {

	private final ShapeRoi shape;

	public ShapeRoiWrapper(final ShapeRoi shape) {
		this.shape = shape;
	}

	@Override
	public boolean test(final RealLocalizable t) {
		// The backing shape is stored with its upper left corner at (0, 0)
		final double x = t.getDoublePosition(0) - shape.getXBase();
		final double y = t.getDoublePosition(1) - shape.getYBase();
		return shape.getShape().contains(x, y);
	}

	@Override
	public double realMin(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? shape.getXBase() : shape.getYBase();
	}

	@Override
	public double realMax(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? shape.getXBase() + shape.getFloatWidth() : shape
			.getYBase() + shape.getFloatHeight();
	}

	@Override
	public ShapeRoi getRoi() {
		return shape;
	}

}
