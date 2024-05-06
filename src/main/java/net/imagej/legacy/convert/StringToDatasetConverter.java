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

import java.lang.reflect.Type;

import org.scijava.convert.AbstractDelegateConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import net.imagej.Dataset;

/**
 * Converts a string that matches an {@link ImagePlus} title or ID to {@link Dataset}.
 * (The {@link ImagePlus} must be known to the {@link ij.WindowManager}. This
 * means it must be visible)
 * <p>
 * Similar to {@link StringToImagePlusConverter} but output type is {@link Dataset}.
 *
 * @author Matthias Arzt
 */
@Plugin(type = Converter.class)
public class StringToDatasetConverter
	extends AbstractDelegateConverter<String, ImagePlus, Dataset>
{
	@Parameter
	private ConvertService convertService;

	private final Converter<String, ImagePlus> toImagePlus =
		new StringToImagePlusConverter();

	@Override
	public boolean canConvert(Object src, Class<?> dest) {
		if (!super.canConvert(src, dest)) return false;
		return srcImageSupported((String) src);
	}

	@Override
	public boolean canConvert(Object src, Type dest) {
		if (!super.canConvert(src, dest)) return false;
		return srcImageSupported((String) src);
	}

	private boolean srcImageSupported(String src) {
		ImagePlus srcImage = toImagePlus.convert(src, getDelegateType());
		return srcImage != null && convertService.supports(srcImage, getOutputType());
	}

	@Override
	public Class<String> getInputType()
	{
		return String.class;
	}

	@Override
	public Class<Dataset> getOutputType()
	{
		return Dataset.class;
	}

	@Override
	protected Class<ImagePlus> getDelegateType() {
		return ImagePlus.class;
	}
}
