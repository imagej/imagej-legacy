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
 * @author Curtis Rueden
 */
@Plugin(type = DisplayViewer.class, priority = Priority.HIGH)
public class LegacyImageDisplayViewer extends AbstractImageDisplayViewer
	implements LegacyDisplayViewer
{

	@Parameter(required = false)
	private LegacyService legacyService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private LogService log;

	// -- Internal AbstractDisplayViewer methods --

	@Override
	protected void updateTitle() {
		// NB: Let's not mess with the ImagePlus title.
	}

	// -- ImageDisplayViewer methods --

	@Override
	public Dataset capture() {
		throw new UnsupportedOperationException();
	}

	// -- DisplayViewer methods --

	@Override
	public boolean canView(final Display<?> d) {
		if (!(d instanceof ImageDisplay) || //
			legacyService == null || //
			!legacyService.isActive() || //
			legacyService.getIJ1Helper() == null || //
			legacyService.getImageMap() == null)
		{
			return false;
		}
		// NB: ImagePlus only supports images with at most five dimensions.
		return getDataset(((ImageDisplay) d).getActiveView()).numDimensions() <= 5;
	}

	@Override
	public void view(final UserInterface ui, final Display<?> d) {
		// NB: Do not create any DisplayWindow!
		view((DisplayWindow) null, d);
		d.update();
	}

	@Override
	public void view(final DisplayWindow w, final Display<?> d) {
		super.view(w, d);

		final ImageDisplay imageDisplay = (ImageDisplay) d;

		// NB: Tell the IJ2 framework what the "active" display is.
		// This allows other consumers to look up the corresponding
		// ImagePlus using the active display.
		displayService.setActiveDisplay(imageDisplay);

		final Data data = imageDisplay.getActiveView().getData();
		if (Dataset.class.isAssignableFrom(data.getClass())) {
			// NB: Check if there is already an ImagePlus that we will
			// associate with this display - even if it hasn't been
			// mapped yet (because the display is still being created!).
			final Dataset dataset = (Dataset) data;
			if (dataset.getProperties().containsKey(LegacyImageMap.IMP_KEY)) return;
		}

		final LegacyImageMap limp = legacyService.getImageMap();

		// if there is already a mapping for this display, just get its ImagePlus
		final ImagePlus existing = limp.lookupImagePlus(imageDisplay);

		// if none: register the display, which triggers wrapping in an ImagePlus
		final ImagePlus imagePlus =
			existing != null ? existing : limp.registerDisplay(imageDisplay);

		// display the ImagePlus via the IJ1 framework
		imagePlus.show();

		// fire an ImageDisplay update event
		d.update();
	}

	@Override
	public boolean isCompatible(final UserInterface ui) {
		return ui instanceof LegacyUI;
	}
}
