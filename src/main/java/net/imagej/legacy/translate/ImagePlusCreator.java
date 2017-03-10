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

package net.imagej.legacy.translate;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;

/**
 * The interface for creating {@link ImagePlus}es from {@link ImageDisplay}s.
 * 
 * @author Barry DeZonia
 */
public interface ImagePlusCreator {

	/**
	 * Creates an {@link ImagePlus} from a {@link ImageDisplay}, as
	 * {@link #createLegacyImage(Dataset, ImageDisplay)}.
	 */
	ImagePlus createLegacyImage(ImageDisplay display);

	/**
	 * Creates an {@link ImagePlus} from a {@link Dataset}, when no
	 * {@link ImageDisplay} is available. This is sufficient but will be less
	 * informative than if a display was provided.
	 */
	ImagePlus createLegacyImage(final Dataset ds);

	/**
	 * Creates an {@link ImagePlus} from a given {@link Dataset} and
	 * {@link ImageDisplay}. The display is optional, but will create a more
	 * robust image.
	 *
	 * @param ds Dataset to use to create the base ImagePlus
	 * @param display OPTIONAL image display
	 * @return The created ImagePlus
	 */
	ImagePlus createLegacyImage(final Dataset ds, final ImageDisplay display);
}
