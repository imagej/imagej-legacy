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

import java.util.Set;

import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;

import org.scijava.ItemVisibility;
import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

/**
 * Registers the module's final input values with ImageJ 1.x's macro recorder.
 * In IJ1 terms, this makes all SciJava modules macro recordable!
 * 
 * @author Curtis Rueden
 */
@Plugin(type = PostprocessorPlugin.class, priority = Priority.VERY_LOW)
public class MacroRecorderPostprocessor extends AbstractPostprocessorPlugin {

	@Parameter(required = false)
	private LegacyService legacyService;

	// -- ModuleProcessor methods --

	@Override
	public void process(final Module module) {
		if (legacyService == null) return;
		if (module.getInfo().is("no-record")) return; // module wants to be skipped
		final IJ1Helper ij1Helper = legacyService.getIJ1Helper();
		if (ij1Helper == null) return;
		if (ij1Helper.isMacro()) return; // do not record while in macro mode

		final Set<String> excludedInputs = //
			MacroRecorderExcludedInputs.retrieve(module);

		for (final ModuleItem<?> input : module.getInfo().inputs()) {
			final String name = input.getName();
			if (excludedInputs != null && excludedInputs.contains(name)) continue;
			if (excludedFromRecording(input)) continue;
			final Object value = module.getInput(name);
			if (value != null) ij1Helper.recordOption(name, toString(value));
		}

		ij1Helper.finishRecording();
	}

	// -- Helper methods --

	private boolean excludedFromRecording(final ModuleItem<?> input) {
		// Skip parameters of insufficient visibility.
		final ItemVisibility visibility = input.getVisibility();
		if (visibility == ItemVisibility.INVISIBLE) return true;
		if (visibility == ItemVisibility.MESSAGE) return true;

		// Skip password parameters.
		final String style = input.getWidgetStyle();
		if (style != null) {
			for (final String s : style.split(",")) {
				if (s.equals(TextWidget.PASSWORD_STYLE)) return true;
			}
		}

		return false;
	}

	private String toString(final Object value) {
		// if object is an ImagePlus, use its title as the string representation
		final String title = IJ1Helper.getTitle(value);
		if (title != null) return title;

		return value.toString();
	}

}
