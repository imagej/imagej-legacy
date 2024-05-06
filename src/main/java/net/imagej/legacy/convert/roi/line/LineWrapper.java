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

package net.imagej.legacy.convert.roi.line;

import net.imagej.legacy.convert.roi.MaskPredicateWrapper;
import net.imglib2.roi.geom.real.WritableLine;

/**
 * A {@link ij.gui.Line} with an associated {@link WritableLine}.
 *
 * @author Alison Walter
 */
public final class LineWrapper extends ij.gui.Line implements
	MaskPredicateWrapper<WritableLine>
{

	private final WritableLine line;

	public LineWrapper(final WritableLine l) {
		super(l.endpointOne().getDoublePosition(0), l.endpointOne()
			.getDoublePosition(1), l.endpointTwo().getDoublePosition(0), l
				.endpointTwo().getDoublePosition(1));
		line = l;
	}

	// -- MaskPredicateWrapper methods --

	@Override
	public WritableLine getSource() {
		return line;
	}

	@Override
	public void synchronize() {
		// TODO: What if the ij.gui.Line has width > 1?
		line.endpointOne().setPosition(new double[] { x1d, y1d });
		line.endpointTwo().setPosition(new double[] { x2d, y2d });
	}

}
