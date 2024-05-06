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

package net.imagej.legacy.plugin;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;

import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractSingleInputPreprocessor;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Plugin;

/**
 * Injects the {@link Overlay} of the current {@link ImagePlus} when a single
 * {@link Overlay} is declared as a parameter.
 * 
 * @author Jan Eglinger
 */
@Plugin(type = PreprocessorPlugin.class, priority = Priority.VERY_HIGH)
public class OverlayPreprocessor extends AbstractSingleInputPreprocessor {

	// -- ModuleProcessor methods --

	@Override
	public void process(final Module module) {
		// add Overlay from imp to single Overlay input
		final String overlayInput = getSingleInput(module, Overlay.class);
		if (overlayInput != null) {
			// TODO: Change this to a LegacyService API call?
			final ImagePlus imp = WindowManager.getCurrentImage();
			if (imp == null)
				return;
			Overlay ovl = imp.getOverlay();
			if (ovl == null)
				return;
			module.setInput(overlayInput, ovl);
			module.resolveInput(overlayInput);
		}
	}
}
