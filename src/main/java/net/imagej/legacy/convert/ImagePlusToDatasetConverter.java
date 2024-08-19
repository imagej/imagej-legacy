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

import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;

import org.scijava.Priority;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * {@link Converter} implementation for converting {@link ImagePlus} to a
 * {@link Dataset}.
 * <p>
 * NB: should be LOWER priority than any default {@code Converter}s to avoid
 * unintentionally grabbing undesired conversions (e.g. involving nulls).
 * </p>
 *
 * @author Mark Hiner
 * @author Curtis Rueden
 */
@Plugin(type = Converter.class, priority = Priority.LOW)
public class ImagePlusToDatasetConverter extends
	AbstractImagePlusLegacyConverter<Dataset>
{

	@Parameter(required = false)
	private ImageDisplayService imageDisplayService;

	// -- Converter methods --

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!legacyEnabled() || imageDisplayService == null) {
			throw new UnsupportedOperationException();
		}

		// Convert using the LegacyImageMap
		final ImageDisplay display =
			legacyService.getImageMap().registerLegacyImage((ImagePlus) src);

		final Dataset dataset = imageDisplayService.getActiveDataset(display);
		return (T) dataset;
	}

	@Override
	public Class<Dataset> getOutputType() {
		return Dataset.class;
	}

	@Override
	public Class<ImagePlus> getInputType() {
		return ImagePlus.class;
	}
}
