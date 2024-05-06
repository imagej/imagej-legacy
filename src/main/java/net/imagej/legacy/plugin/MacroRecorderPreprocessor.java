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

import org.scijava.MenuEntry;
import org.scijava.MenuPath;
import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.module.process.ServicePreprocessor;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Remembers which inputs are resolved extremely early in the preprocessing
 * chain. These inputs will be excluded from ImageJ macro recording by the
 * {@link MacroRecorderPostprocessor}.
 * <p>
 * In particular, we want to exclude {@link org.scijava.service.Service} and
 * {@link org.scijava.Context} parameters, which make no sense to include in
 * recorded macro strings. Such parameters are always populated by the
 * {@link ServicePreprocessor} with {@code 2 * VERY_HIGH} priority. As such,
 * this preprocessor runs at priority {@code VERY_HIGH + 1}: sooner than
 * {@code VERY_HIGH} priority processors, but after the
 * {@link ServicePreprocessor}. This behavior allows other such "pre-resolved"
 * parameters to be excluded from recorded macros in an extensible way, while
 * retaining parameters that get resolved later in the preprocessing chain.
 * </p>
 * 
 * @author Curtis Rueden
 */
@Plugin(type = PreprocessorPlugin.class, priority = Priority.VERY_HIGH + 1)
public class MacroRecorderPreprocessor extends AbstractPreprocessorPlugin {

	@Parameter(required = false)
	private LegacyService legacyService;

	// -- ModuleProcessor methods --

	@Override
	public void process(final Module module) {
		if (legacyService == null) return;
		if (module.getInfo().is("no-record")) return; // module wants to be skipped
		final IJ1Helper ij1Helper = legacyService.getIJ1Helper();
		if (ij1Helper == null) return;
		if (ij1Helper.isMacro()) return;

		ij1Helper.startRecording(menuLabel(module));

		final Set<String> excludedInputs = //
			MacroRecorderExcludedInputs.create(module);

		for (final ModuleItem<?> input : module.getInfo().inputs()) {
			final String name = input.getName();
			if (module.isInputResolved(name)) excludedInputs.add(name);
		}
	}

	// -- Helper methods --

	private String menuLabel(final Module module) {
		final MenuPath menuPath = module.getInfo().getMenuPath();
		if (menuPath != null) {
			final MenuEntry menuLeaf = menuPath.getLeaf();
			if (menuLeaf != null) return menuLeaf.getName();
		}
		return module.getInfo().getTitle();
	}
}
