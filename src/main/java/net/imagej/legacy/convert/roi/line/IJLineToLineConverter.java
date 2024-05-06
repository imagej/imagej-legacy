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

import ij.gui.Arrow;

import net.imagej.legacy.convert.roi.AbstractRoiToMaskPredicateConverter;
import net.imglib2.roi.geom.real.Line;

import org.scijava.convert.Converter;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Converts an ImageJ 1.x {@link Line} to an ImgLib2 {@link Line}. This is only
 * intended to convert ImageJ 1.x {@link Line}s with {@code width <= 1}.
 *
 * @author Alison Walter
 */
@Plugin(type = Converter.class)
public class IJLineToLineConverter extends
	AbstractRoiToMaskPredicateConverter<ij.gui.Line, Line>
{

	@Parameter(required = false)
	private LogService log;

	@Override
	public boolean canConvert(final Class<?> src, final Class<?> dest) {
		return super.canConvert(src, dest) && !src.equals(Arrow.class);
	}

	@Override
	public Class<Line> getOutputType() {
		return Line.class;
	}

	@Override
	public Class<ij.gui.Line> getInputType() {
		return ij.gui.Line.class;
	}

	@Override
	public Line convert(final ij.gui.Line src) {
		if (log != null && ij.gui.Line.getWidth() > 1) {
			log.warn("Ignoring line width >1.");
		}
		return new IJLineWrapper(src);
	}

	@Override
	public boolean supportedType(final ij.gui.Line src) {
		return !(src instanceof Arrow);
	}

}
