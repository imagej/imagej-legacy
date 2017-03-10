/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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
import java.util.Collection;

import net.imagej.display.ImageDisplay;
import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;

import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ConversionUtils;
import org.scijava.util.GenericUtils;

import ij.ImagePlus;

/**
 * {@link Converter} implementation for converting {@link ImagePlus} to a
 * {@link ImageDisplay}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Converter.class, priority = Priority.LOW_PRIORITY)
public class ImagePlusToImageDisplayConverter extends
	AbstractConverter<ImagePlus, ImageDisplay>
{

	@Parameter(required = false)
	private LegacyService legacyService;

	// -- Converter methods --

	@Override
	public boolean canConvert(final Class<?> src, final Type dest) {
		return canConvert(src, GenericUtils.getClass(dest));
	}

	@Override
	public boolean canConvert(final Class<?> src, final Class<?> dest) {
		if (legacyService == null) return false;
		return legacyService.getIJ1Helper().isImagePlus(src) &&
			ConversionUtils.canCast(dest, ImageDisplay.class);
	}

	@Override
	public boolean canConvert(final Object src, final Type dest) {
		return canConvert(src.getClass(), dest);
	}

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		return canConvert(src.getClass(), dest);
	}

	@Override
	public Object convert(final Object src, final Type dest) {
		return convert(src, GenericUtils.getClass(dest));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(final Object src, final Class<T> dest) {
		if (legacyService == null) throw new UnsupportedOperationException();

		// Convert using the LegacyImageMap
		final ImageDisplay display =
			legacyService.getImageMap().registerLegacyImage((ImagePlus) src);

		return (T) display;
	}

	@Override
	public void populateInputCandidates(final Collection<Object> objects) {
		if (legacyService == null) return;

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
