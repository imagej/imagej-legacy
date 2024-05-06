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

import org.scijava.convert.AbstractConverter;

/**
 * Abstract base class for converters which unwrap wrapped ImageJ 1.x Rois.
 *
 * @author Alison Walter
 */
public abstract class AbstractRoiUnwrapConverter<R extends Roi> extends
	AbstractConverter<IJRoiWrapper<?, ?>, R>
{

	@Override
	public boolean canConvert(final Object src, final Type dest) {
		if (src instanceof IJRoiWrapper) return getOutputType().isInstance(
			((IJRoiWrapper<?, ?>) src).getRoi()) && super.canConvert(src, dest);
		return false;
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		if (src instanceof IJRoiWrapper) return getOutputType().isInstance(
			((IJRoiWrapper<?, ?>) src).getRoi()) && super.canConvert(src, dest);
		return false;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Class<IJRoiWrapper<?, ?>> getInputType() {
		return (Class) IJRoiWrapper.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!(src instanceof IJRoiWrapper)) throw new IllegalArgumentException(
			"Cannot convert " + src.getClass() + " to " + getOutputType());

		return (T) ((IJRoiWrapper<?, ?>) src).getRoi();
	}

}
