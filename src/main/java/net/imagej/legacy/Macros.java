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

package net.imagej.legacy;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.measure.ResultsTable;

import java.util.Collections;

import org.scijava.module.Module;

/**
 * Static utility methods for working with ImageJ 1.x macros via its
 * <a href="https://imagej.net/developer/macro/functions.html#call">call</a>
 * function.
 *
 * @author Curtis Rueden
 */
public final class Macros {

	private Macros() {
		// NB: Prevent instantiation of utility class.
	}

	private static ThreadLocal<Module> activeModule = new ThreadLocal<>();

	public static Module getActiveModule() {
		return activeModule.get();
	}

	public static void setActiveModule(final Module m) {
		activeModule.set(m);
	}

	public static void setActiveImage(final String name) {
		final Module m = getActiveModule();
		if (m == null) return;
		final Object value = m.getInput(name);
		if (value == null || !(value instanceof ImagePlus)) return;
		WindowManager.setTempCurrentImage((ImagePlus) value);
	}

	public static void attachResultsTable() {
		final ResultsTable results = ResultsTable.getResultsTable();
		if (results == null) return;
		final ImagePlus activeImage = IJ.getImage();
		if (activeImage == null) return;
		activeImage.setProperty("tables", Collections.singletonList(results));
	}
}
