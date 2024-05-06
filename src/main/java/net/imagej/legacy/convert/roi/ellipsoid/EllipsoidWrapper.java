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

import net.imagej.legacy.convert.roi.MaskPredicateWrapper;
import net.imglib2.RealPositionable;
import net.imglib2.roi.geom.real.WritableEllipsoid;

/**
 * A {@link OvalRoi} with an associated {@link WritableEllipsoid}.
 *
 * @author Alison Walter
 */
public final class EllipsoidWrapper extends OvalRoi implements
	MaskPredicateWrapper<WritableEllipsoid>
{

	private final WritableEllipsoid ellipsoid;

	public EllipsoidWrapper(final WritableEllipsoid e) {
		super(e.center().getDoublePosition(0) - e.semiAxisLength(0), e.center()
			.getDoublePosition(1) - e.semiAxisLength(1), e.semiAxisLength(0) * 2, e
				.semiAxisLength(1) * 2);
		ellipsoid = e;
	}

	// -- MaskPredicateWrapper methods --

	@Override
	public WritableEllipsoid getSource() {
		return ellipsoid;
	}

	@Override
	public void synchronize() {
		final RealPositionable ellipsoidCenter = ellipsoid.center();
		ellipsoidCenter.setPosition(getXBase() + (getFloatWidth() / 2.0), 0);
		ellipsoidCenter.setPosition(getYBase() + (getFloatHeight() / 2.0), 1);
		ellipsoid.setSemiAxisLength(0, getFloatWidth() / 2.0);
		ellipsoid.setSemiAxisLength(1, getFloatHeight() / 2.0);
	}

}
