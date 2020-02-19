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
import ij.WindowManager;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Converts a string to the corresponding {@link ImagePlus} object. Both image
 * titles and ID values are supported (with IDs being preferred).
 * <p>
 * This converter exists to make ImageJ 1.x macro calls to SciJava modules work
 * better with {@link ImagePlus} inputs&mdash;e.g.:
 * </p>
 * 
 * <pre>
 * run(&quot;Print Title&quot;, &quot;imp=blobs.gif name=Jan&quot;);
 * </pre>
 * <p>
 * The converter enables string fragments like {@code imp=blobs.gif} or
 * {@code imp=-2} to properly resolve to the associated {@link ImagePlus}.
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
public class StringToImagePlusConverter extends
	AbstractConverter<String, ImagePlus>
{

	// -- Converter methods --

	@Override
	public boolean canConvert(final Object src, final Class<?> dest) {
		return convert(src, ImagePlus.class) != null;
	}

	@Override
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!(src instanceof String)) return null;
		final String s = (String) src;
		try {
			final int imageID = Integer.parseInt(s);
			final ImagePlus imp = WindowManager.getImage(imageID);
			if (imp != null) {
				@SuppressWarnings("unchecked")
				final T typedImp = (T) imp;
				return typedImp;
			}
		}
		catch (final NumberFormatException exc) {
			// NB: Not a valid image ID; try image title.
		}
		final ImagePlus imp = WindowManager.getImage(s);
		@SuppressWarnings("unchecked")
		final T typedImp = (T) imp;
		return typedImp;
	}

	@Override
	public Class<ImagePlus> getOutputType() {
		return ImagePlus.class;
	}

	@Override
	public Class<String> getInputType() {
		return String.class;
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
