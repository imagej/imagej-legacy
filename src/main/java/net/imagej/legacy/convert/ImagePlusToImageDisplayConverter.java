/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2020 ImageJ developers.
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

import java.util.Collection;

import net.imagej.display.ImageDisplay;
import net.imagej.legacy.IJ1Helper;

import org.scijava.Priority;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * {@link Converter} implementation for converting {@link ImagePlus} to a
 * {@link ImageDisplay}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Converter.class, priority = Priority.LOW)
public class ImagePlusToImageDisplayConverter extends
	AbstractLegacyConverter<ImagePlus, ImageDisplay>
{

	// -- Converter methods --

	@Override
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!legacyEnabled()) throw new UnsupportedOperationException();

		// Convert using the LegacyImageMap
		final ImageDisplay display =
			legacyService.getImageMap().registerLegacyImage((ImagePlus) src);

		@SuppressWarnings("unchecked")
		final T typedDisplay = (T) display;
		return typedDisplay;
	}

	@Override
	public void populateInputCandidates(final Collection<Object> objects) {
		if (!legacyEnabled()) return;

		final IJ1Helper ij1Helper = legacyService.getIJ1Helper();

		final int[] imageIDs = ij1Helper.getIDList();
		if (imageIDs == null) return; // no image IDs

		// Add any ImagePluses in the IJ1 WindowManager that are not already
		// converted
		for (final int id : imageIDs) {
			final ImagePlus imp = ij1Helper.getImage(id);
			if (legacyService.getImageMap().lookupDisplay(imp) == null) {
				objects.add(imp);
			}
		}
	}

	@Override
	public Class<ImageDisplay> getOutputType() {
		return ImageDisplay.class;
	}

	@Override
	public Class<ImagePlus> getInputType() {
		return ImagePlus.class;
	}
}
