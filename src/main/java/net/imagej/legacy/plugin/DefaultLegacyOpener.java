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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import net.imagej.Dataset;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;

import org.scijava.Context;
import org.scijava.Priority;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Plugin;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object open(final String path, final int planeIndex,
		final boolean displayResult)
	{
		Context c = (Context) IJ.runPlugIn("org.scijava.Context", null);

		CommandService cmd = c.getService(CommandService.class);
		Map<String, Object> inputs = new HashMap<String, Object>();
		if (path != null) inputs.put("inputFile", new File(path));
		Future<CommandModule> result = cmd.run(OpenFile.class, true, inputs);

		CommandModule module = c.getService(ModuleService.class).waitFor(result);
		Object data = module.getOutput("data");

		if (data instanceof Dataset) {
			Dataset d = (Dataset) data;
			ImagePlus imp =
				ImageJFunctions.wrap((RandomAccessibleInterval) d.getImgPlus(), d
					.getName());

			if (displayResult) {
				imp.show();
			}
			return imp;
		}

		return null;
	}
}
