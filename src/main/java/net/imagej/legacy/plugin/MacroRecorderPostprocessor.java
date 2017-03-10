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

package net.imagej.legacy.plugin;

import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;

import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Registers the module's final input values with ImageJ 1.x's macro recorder.
 * In IJ1 terms, this makes all SciJava modules macro recordable!
 * 
 * @author Curtis Rueden
 */
@Plugin(type = PostprocessorPlugin.class, priority = Priority.VERY_LOW_PRIORITY)
public class MacroRecorderPostprocessor extends AbstractPostprocessorPlugin {

	@Parameter(required = false)
	private LegacyService legacyService;

	// -- ModuleProcessor methods --

	@Override
	public void process(final Module module) {
		final IJ1Helper ij1Helper = legacyService.getIJ1Helper();
		if (ij1Helper == null) return;
		if (ij1Helper.isMacro()) {
			// do not record parameters while in macro mode
			return;
		}
		for (final ModuleItem<?> input : module.getInfo().inputs()) {
			final String name = input.getName();
			final Object value = module.getInput(name);
			if (value != null) ij1Helper.recordOption(name, toString(value));
		}
	}

	// -- Helper methods --

	private String toString(final Object value) {
		// if object is an ImagePlus, use its title as the string representation
		final String title = IJ1Helper.getTitle(value);
		if (title != null) return title;

		return value.toString();
	}

}
