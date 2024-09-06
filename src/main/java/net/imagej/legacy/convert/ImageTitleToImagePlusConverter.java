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

import java.util.Collection;

import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.ObjectWidget;
import org.scijava.widget.WidgetModel;

import ij.ImagePlus;
import ij.WindowManager;

/**
 * Converts an image title to the corresponding {@link ImagePlus} object.
 * <p>
 * This converter is a hack to expose available {@link ImagePlus} objects as
 * choices for the widget framework. It allows {@link ObjectWidget}
 * implementations to display the available {@link ImagePlus} objects by title.
 * </p>
 * <p>
 * Note that unlike most other classes in the ImageJ Legacy project, this one
 * <em>needs</em> typed references to classes in the {@code ij} package,
 * particularly {@link ImagePlus}, so that the conversion logic works as
 * intended. It seems to work without side effects in the standard case...
 * </p>
 *
 * @author Curtis Rueden
 * @see ConvertService#getCompatibleInputs(Class)
 * @see WidgetModel#getObjectPool()
 */
@Plugin(type = Converter.class)
public class ImageTitleToImagePlusConverter extends
	AbstractLegacyConverter<ImageTitleToImagePlusConverter.ImageTitle, ImagePlus>
{

	// -- Converter methods --

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!legacyEnabled()) throw new UnsupportedOperationException();
		return src instanceof ImageTitle ? (T) ((ImageTitle) src).imp : null;
	}

	@Override
	public void populateInputCandidates(final Collection<Object> objects) {
		final int[] imageIDs = WindowManager.getIDList();
		if (imageIDs == null) return;
		for (final int imageID : imageIDs) {
			final ImagePlus imp = WindowManager.getImage(imageID);
			if (imp != null) objects.add(new ImageTitle(imp));
		}
	}

	@Override
	public Class<ImagePlus> getOutputType() {
		return ImagePlus.class;
	}

	@Override
	public Class<ImageTitle> getInputType() {
		return ImageTitle.class;
	}

	// -- Helper classes --

	/**
	 * Adapter for {@link ImagePlus} that emits the image title when
	 * {@link #toString()} is called.
	 *
	 * @author Curtis Rueden
	 */
	public static class ImageTitle {

		private final ImagePlus imp;

		public ImageTitle(final ImagePlus imp) {
			this.imp = imp;
		}

		// -- Object methods --

		@Override
		public String toString() {
			return imp.getTitle();
		}

	}

}
