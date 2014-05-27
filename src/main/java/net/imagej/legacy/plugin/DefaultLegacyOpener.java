/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2014 Board of Regents of the University of
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

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.legacy.LegacyService;
import net.imagej.legacy.translate.DefaultImageTranslator;
import net.imagej.legacy.translate.ImageTranslator;

import org.scijava.Cancelable;
import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayPostprocessor;
import org.scijava.display.DisplayService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.plugins.commands.io.OpenFile;

/**
 * The default {@link LegacyOpener} plugin.
 * <p>
 * When the {@link net.imagej.legacy.DefaultLegacyHooks} are installed, the
 * {@link LegacyOpener} plugin (if any) is given a chance to handle various
 * {@code open} requests from ImageJ 1.x by using IJ2.
 * </p>
 * 
 * @author Mark Hiner
 */
@Plugin(type = LegacyOpener.class, priority = Priority.LOW_PRIORITY)
public class DefaultLegacyOpener implements LegacyOpener {

	@Override
	public Object open(final String path, final int planeIndex,
		final boolean displayResult)
	{
		final Context c = (Context) IJ.runPlugIn("org.scijava.Context", null);
		ImagePlus imp = null;

		final PluginService pluginService = c.getService(PluginService.class);
		final LegacyService legacyService = c.getService(LegacyService.class);
		final DisplayService displayService = c.getService(DisplayService.class);

		final List<PostprocessorPlugin> postprocessors =
			new ArrayList<PostprocessorPlugin>();
		for (final PostprocessorPlugin pp : pluginService
			.createInstancesOfType(PostprocessorPlugin.class))
		{
			// If we're not supposed to display the result, remove any
			// DisplayPostprocessors
			if (displayResult || !(pp instanceof DisplayPostprocessor)) {
				postprocessors.add(pp);
			}
		}

		// Run the OpenFile command to get our data
		final CommandService commandService = c.getService(CommandService.class);
		final CommandInfo command = commandService.getCommand(OpenFile.class);
		final ModuleService moduleService = c.getService(ModuleService.class);
		final Map<String, Object> inputs = new HashMap<String, Object>();
		if (path != null) inputs.put("inputFile", new File(path));
		final Future<Module> result =
			moduleService.run(command, pluginService
				.createInstancesOfType(PreprocessorPlugin.class), postprocessors,
				inputs);

		final Module module = moduleService.waitFor(result);
		if (module == null) return null;
		if (Cancelable.class.isAssignableFrom(module.getClass())) {
			if (((Cancelable)module).isCanceled()) {
				return Boolean.TRUE;
			}
		}

		final Object data = module.getOutput("data");

		if (data != null && data instanceof Dataset) {
			if (displayResult) {
				// Image was displayed during the command execution, so we just get the
				// ImageDisplay and lookup its ImagePlus
				final ImageDisplay imageDisplay =
					displayService.getActiveDisplay(ImageDisplay.class);
				imp = legacyService.getImageMap().lookupImagePlus(imageDisplay);
			}
			else {
				// Need to manually register the ImagePlus and return it
				final Dataset d = (Dataset) data;
				final ImageTranslator it = new DefaultImageTranslator(legacyService);
				imp = it.createLegacyImage(d);
			}
		}

		return imp;
	}
}
