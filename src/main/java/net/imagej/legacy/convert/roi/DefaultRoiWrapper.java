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

import ij.gui.Roi;

import java.awt.Rectangle;

import net.imglib2.Localizable;
import net.imglib2.Positionable;
import net.imglib2.RealPositionable;
import net.imglib2.roi.MaskInterval;

/**
 * Wraps any {@link Roi} as a {@link MaskInterval}. The {@code test(...)} method
 * of this class, simply calls {@code contains(...)} on the underlying ImageJ
 * 1.x Roi. This is intended to wrap existing ImageJ 1.x Rois which do not
 * translate well to existing Imglib2 interfaces.
 *
 * @author Alison Walter
 */
public class DefaultRoiWrapper<R extends Roi> implements
	IJRoiWrapper<R, Localizable>, MaskInterval
{

	private final R roi;

	/**
	 * Creates a {@link MaskInterval} which wraps the given {@link Roi}.
	 *
	 * @param roi the Roi to be wrapped
	 */
	public DefaultRoiWrapper(final R roi) {
		this.roi = roi;
	}

	@Override
	public int numDimensions() {
		return 2;
	}

	@Override
	public boolean test(final Localizable t) {
		return roi.contains(t.getIntPosition(0), t.getIntPosition(1));
	}

	@Override
	public long min(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? roi.getBounds().x : roi.getBounds().y;
	}

	@Override
	public void min(final long[] min) {
		min[0] = min(0);
		min[1] = min(1);
	}

	@Override
	public void min(final Positionable min) {
		min.setPosition(min(0), 0);
		min.setPosition(min(1), 1);
	}

	@Override
	public long max(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		final Rectangle r = roi.getBounds();
		return d == 0 ? r.x + r.width : r.y + r.height;
	}

	@Override
	public void max(final long[] max) {
		max[0] = max(0);
		max[1] = max(1);
	}

	@Override
	public void max(final Positionable max) {
		max.setPosition(max(0), 0);
		max.setPosition(max(1), 1);
	}

	@Override
	public double realMin(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? roi.getXBase() : roi.getYBase();
	}

	@Override
	public void realMin(final double[] min) {
		min[0] = realMin(0);
		min[1] = realMin(1);
	}

	@Override
	public void realMin(final RealPositionable min) {
		min.setPosition(realMin(0), 0);
		min.setPosition(realMin(1), 1);
	}

	@Override
	public double realMax(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? roi.getXBase() + roi.getFloatWidth() : roi.getYBase() + roi
			.getFloatHeight();
	}

	@Override
	public void realMax(final double[] max) {
		max[0] = realMax(0);
		max[1] = realMax(1);
	}

	@Override
	public void realMax(final RealPositionable max) {
		max.setPosition(realMax(0), 0);
		max.setPosition(realMax(1), 1);
	}

	@Override
	public void dimensions(final long[] dimensions) {
		dimensions[0] = dimension(0);
		dimensions[1] = dimension(1);
	}

	@Override
	public long dimension(final int d) {
		if (d != 0 && d != 1) throw new IllegalArgumentException(
			"Invalid dimension " + d);
		return d == 0 ? roi.getBounds().width : roi.getBounds().height;
	}

	@Override
	public R getRoi() {
		return roi;
	}

}
