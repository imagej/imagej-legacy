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

import ij.gui.PointRoi;

import net.imagej.legacy.convert.roi.MaskPredicateWrapper;
import net.imglib2.roi.geom.real.WritablePointMask;

/**
 * A {@link PointRoi} with an associated {@link WritablePointMask}.
 *
 * @author Alison Walter
 */
public final class PointMaskWrapper extends PointRoi implements
	MaskPredicateWrapper<WritablePointMask>
{

	private final WritablePointMask pointMask;

	public PointMaskWrapper(final WritablePointMask p) {
		super(p.getDoublePosition(0), p.getDoublePosition(1));
		pointMask = p;
	}

	// -- MaskPredicateWrapper methods --

	@Override
	public WritablePointMask getSource() {
		return pointMask;
	}

	@Override
	public void synchronize() {
		// TODO: What happens if the PointRoi has multiple points?
		pointMask.setPosition(getXBase() + xpf[0], 0);
		pointMask.setPosition(getYBase() + ypf[0], 1);
	}

}
