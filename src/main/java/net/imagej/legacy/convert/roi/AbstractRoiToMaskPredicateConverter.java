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

import java.lang.reflect.Type;

import net.imglib2.RealLocalizable;
import net.imglib2.roi.MaskPredicate;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConversionRequest;

/**
 * Base class for all converters which convert {@link Roi} to
 * {@link MaskPredicate}.
 *
 * @author Alison Walter
 * @param <R> ImageJ 1.x {@link Roi} input type
 * @param <M> {@link MaskPredicate} output type
 */
public abstract class AbstractRoiToMaskPredicateConverter<R extends Roi, M extends MaskPredicate<? extends RealLocalizable>>
	extends AbstractConverter<R, M>
{

	@Override
	@SuppressWarnings("unchecked")
	public boolean canConvert(final ConversionRequest request) {
		if (super.canConvert(request)) return supportedType((R) request
			.sourceObject());
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean canConvert(final Object src, final Type dest) {
		if (super.canConvert(src, dest)) return supportedType((R) src);
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (super.canConvert(src, dest)) return supportedType((R) src);
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!getInputType().isInstance(src)) throw new IllegalArgumentException(
			"Cannot convert " + src.getClass().getSimpleName() + " to " +
				getOutputType().getSimpleName());

		return (T) convert((R) src);
	}

	public abstract M convert(final R src);

	public abstract boolean supportedType(R src);
}
