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

package net.imagej.legacy.translate;

import ij.ImagePlus;

import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.legacy.LegacyService;

import org.scijava.AbstractContextual;
import org.scijava.Context;

/**
 * Combines {@link DisplayCreator} and {@link ImagePlusCreator}.
 *
 * @author Barry DeZonia
 * @author Curtis Rueden
 */
public class ImageTranslator extends AbstractContextual
{

	private final DisplayCreator displayCreator;
	private final ImagePlusCreator imagePlusCreator;

	public ImageTranslator(final LegacyService legacyService) {
		final Context context = legacyService.getContext();
		displayCreator = new DisplayCreator(context);
		imagePlusCreator = new ImagePlusCreator(context);
	}

	/**
	 * Creates a {@link ImageDisplay} from an {@link ImagePlus}. Shares planes of
	 * data when possible.
	 */
	public ImageDisplay createDisplay(final ImagePlus imp) {
		return displayCreator.createDisplay(imp);
	}

	/**
	 * Creates an {@link ImagePlus} from a {@link ImageDisplay}. Shares planes of
	 * data when possible.
	 */
	public ImagePlus createLegacyImage(final ImageDisplay display) {
		return imagePlusCreator.createLegacyImage(display);
	}

	public ImagePlus createLegacyImage(final Dataset ds) {
		return imagePlusCreator.createLegacyImage(ds);
	}

	public ImagePlus createLegacyImage(final Dataset ds,
		final ImageDisplay display)
	{
		return imagePlusCreator.createLegacyImage(ds, display);
	}
}
