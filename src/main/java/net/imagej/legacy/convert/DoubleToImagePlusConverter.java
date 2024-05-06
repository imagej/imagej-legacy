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

package net.imagej.legacy.convert;

import ij.ImagePlus;
import ij.WindowManager;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Converts an image ID in {@code double} form to its corresponding
 * {@link ImagePlus} object.
 * <p>
 * ImageJ 1.x macros do not support object references. To reference an image,
 * you must either do so by title (a string) or by ID (a double). If a macro
 * declares an output of type {@link ImagePlus}, the macro must assign that
 * output with either a title or an ID. This converter exists so that ImageJ 1.x
 * macros that assign a double ID value to an {@link ImagePlus} output will be
 * properly converted when the actual output value is needed.
 * </p>
 * <p>
 * Note that unlike most other classes in the ImageJ Legacy project, this one
 * <em>needs</em> typed references to classes in the {@code ij} package,
 * particularly {@link ImagePlus}, so that the conversion logic works as
 * intended. It seems to work without side effects in the standard case...
 * </p>
 *
 * @author Curtis Rueden
 */
@Plugin(type = Converter.class)
public class DoubleToImagePlusConverter extends
	AbstractLegacyConverter<Double, ImagePlus>
{

	// -- Converter methods --

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		return legacyEnabled() && convert(src, ImagePlus.class) != null;
	}

	@Override
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!legacyEnabled()) throw new UnsupportedOperationException();
		if (!(src instanceof Double)) return null;
		final Double imageID = (Double) src;
		final ImagePlus imp = WindowManager.getImage(imageID.intValue());
		@SuppressWarnings("unchecked")
		final T typedImp = (T) imp;
		return typedImp;
	}

	@Override
	public Class<ImagePlus> getOutputType() {
		return ImagePlus.class;
	}

	@Override
	public Class<Double> getInputType() {
		return Double.class;
	}
}
