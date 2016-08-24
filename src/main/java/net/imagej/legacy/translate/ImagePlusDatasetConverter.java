/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
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

package net.imagej.legacy.translate;

import ij.ImagePlus;

import java.lang.reflect.Type;
import java.util.Collection;

import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.display.ImageDisplayService;
import net.imagej.legacy.LegacyService;
import net.imagej.legacy.IJ1Helper;
import org.scijava.Priority;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ConversionUtils;
import org.scijava.util.GenericUtils;

/**
 * {@link Converter} implementation for converting {@link ImagePlus} to a
 * {@link Dataset}.
 * <p>
 * NB: should be LOWER priority than any default {@code Converter}s to avoid
 * unintentionally grabbing undesired conversions (e.g. involving nulls).
 * </p>
 *
 * @author Mark Hiner
 */
@Plugin(type = Converter.class, priority = Priority.LOW_PRIORITY)
public class ImagePlusDatasetConverter extends
	AbstractConverter<ImagePlus, Dataset>
{

	@Parameter
	private ImageDisplayService imageDisplayService;

	@Parameter
	private LegacyService legacyService;

	@Parameter
	private ObjectService objectService;

	// -- Converter methods --

	@Override
	public boolean canConvert(final Class<?> src, final Type dest) {
		return canConvert(src, GenericUtils.getClass(dest));
	}

	@Override
	public boolean canConvert(final Class<?> src, final Class<?> dest) {
		return ConversionUtils.canCast(src, ImagePlus.class) &&
			ConversionUtils.canCast(dest, Dataset.class);
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
		// Convert using the LegacyImageMap
		final ImageDisplay display =
			legacyService.getImageMap().registerLegacyImage((ImagePlus) src);

		final Dataset dataset = imageDisplayService.getActiveDataset(display);
		return (T) dataset;
	}

	@Override
	public void populateInputCandidates(final Collection<Object> objects) {
		final IJ1Helper ij1Helper = legacyService.getIJ1Helper();

		final int[] imageIDs = ij1Helper.getIDList();
		if (imageIDs == null) return; // no image IDs

		// Add any ImagePluses in the IJ1 WindowManager that are not already
		// converted
		for (final int id : imageIDs) {
			final ImagePlus imgPlus = ij1Helper.getImage(id);
			if (legacyService.getImageMap().lookupDisplay(imgPlus) == null) {
				objects.add(imgPlus);
			}
		}
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
