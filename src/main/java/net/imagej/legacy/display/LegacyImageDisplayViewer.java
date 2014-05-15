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

package net.imagej.legacy.display;

import ij.ImagePlus;
import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.legacy.LegacyImageMap;
import net.imagej.legacy.LegacyService;
import net.imagej.legacy.ui.LegacyUI;
import net.imagej.ui.viewer.image.AbstractImageDisplayViewer;
import net.imagej.ui.viewer.image.ImageDisplayViewer;

import org.scijava.Priority;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UserInterface;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;

/**
 * Default {@link ImageDisplayViewer} implementation for viewing the IJ2
 * {@link Dataset} and {@link Data} outputs via an {@link ImagePlus}.
 *
 * @author Mark Hiner
 */
@Plugin(type = DisplayViewer.class, priority = Priority.HIGH_PRIORITY)
public class LegacyImageDisplayViewer extends AbstractImageDisplayViewer
	implements LegacyDisplayViewer
{

	@Parameter
	LegacyService legacyService;

	@Parameter
	DisplayService displayService;

	@Parameter
	LogService logService;

	// -- DisplayViewer methods --

	@Override
	public void view(final DisplayWindow w, final Display<?> d) {
		final LegacyImageMap limp = legacyService.getImageMap();
		ImageDisplay imageDisplay = null;
		ImagePlus imagePlus = null;

		// We can only handle ImageDisplays right now
		if (d instanceof ImageDisplay) {
			imageDisplay = (ImageDisplay) d;
		}
		else {
			logService
				.error("LegacyImageDisplayViewer can not handle Displays of type: " +
					d.getClass() + ". Only ImageDisplays.");
			return;
		}

		// If there is already a mapping for this display, just get its ImagePlus.
		imagePlus = limp.lookupImagePlus(imageDisplay);

		// Otherwise, we register the display, which triggers wrapping in an
		// ImagePlus
		if (imagePlus == null) {
			imagePlus = limp.registerDisplay(imageDisplay);
		}

		// Display the ImagePlus via the IJ1 framework.
		imagePlus.show();

		// Need to tell the IJ2 framework what the "active" display is. This allows
		// other consumers to look up the corresponding ImagePlus using the
		// active display.
		displayService.setActiveDisplay(imageDisplay);
	}

	@Override
	public Dataset capture() {
		// TODO
		throw new UnsupportedOperationException(
			"LegacyImageDisplayViewer#capture not yet implemented.");
	}

	@Override
	public boolean isCompatible(final UserInterface ui) {
		return ui instanceof LegacyUI;
	}

}
