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

import net.imagej.legacy.ui.LegacyUI;

import org.scijava.display.Display;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UserInterface;
import org.scijava.ui.viewer.AbstractDisplayViewer;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;

/**
 * {@link DisplayViewer} implementation for {@link ImagePlus}. Compatible with
 * the {@link LegacyUI}.
 * 
 * @author Mark Hiner
 * @author Curtis Rueden
 */
@Plugin(type = DisplayViewer.class)
public class ImagePlusDisplayViewer extends AbstractDisplayViewer<ImagePlus> {

	// -- Internal AbstractDisplayViewer methods --

	@Override
	protected void updateTitle() {
		// NB: Let's not mess with the ImagePlus title.
	}

	// -- DisplayViewer methods --

	@Override
	public boolean isCompatible(final UserInterface ui) {
		return ui instanceof LegacyUI;
	}

	@Override
	public boolean canView(final Display<?> d) {
		return d instanceof ImagePlusDisplay;
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
		final ImagePlusDisplay display = (ImagePlusDisplay) d;
		for (final ImagePlus imp : display) {
			imp.show();
		}
	}
}
