/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2025 ImageJ2 developers.
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

import net.imagej.legacy.convert.roi.AbstractMaskPredicateToRoiConverter;
import net.imglib2.roi.geom.real.Box;

import org.scijava.Priority;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Converts a {@link Box} to a {@link Roi}. The boundary behavior of this
 * {@code Box} will be lost as a result of this conversion.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class, priority = Priority.LOW)
public class BoxToRoiConverter extends
	AbstractMaskPredicateToRoiConverter<Box, Roi>
{

	@Override
	public Class<Roi> getOutputType() {
		return Roi.class;
	}

	@Override
	public Class<Box> getInputType() {
		return Box.class;
	}

	@Override
	public Roi convert(final Box mask) {
		return new Roi(mask.realMin(0), mask.realMin(1), mask.sideLength(0), mask
			.sideLength(1));
	}

	@Override
	public boolean isLossy() {
		return true;
	}
}
