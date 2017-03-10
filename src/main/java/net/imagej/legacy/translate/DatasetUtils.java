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

import net.imagej.Dataset;
import net.imglib2.Dimensions;

/**
 * Utility methods applicable to {@link Dataset}s.
 * 
 * @author Barry DeZonia
 */
public class DatasetUtils {

	// -- constructor --

	private DatasetUtils() {
		// do not instantiate utility class
	}

	// -- public interface --

	// TODO - move this code into DatasetService as default behavior????

	/**
	 * Allocates the color table array within a new planar Dataset (one table per
	 * plane). Each color table will be assigned null.
	 * 
	 * @param ds The Dataset to modify
	 */
	public static void initColorTables(final Dataset ds) {
		final long numPlanes = planeCount(ds);
		if (numPlanes > Integer.MAX_VALUE) throw new IllegalArgumentException(
			"color table count cannot exceed " + Integer.MAX_VALUE);
		ds.getImgPlus().initializeColorTables((int) numPlanes);
	}

	// -- private helpers --

	private static long planeCount(final Dimensions dims) {
		if (dims.numDimensions() < 2) return 0;
		if (dims.numDimensions() == 2) return 1;
		long count = 1;
		for (int i = 2; i < dims.numDimensions(); i++)
			count *= dims.dimension(i);
		return count;
	}

}
