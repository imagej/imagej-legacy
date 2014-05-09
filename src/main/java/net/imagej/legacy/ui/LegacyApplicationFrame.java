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
package net.imagej.legacy.ui;

import ij.IJ;
import ij.ImageJ;

import org.scijava.ui.ApplicationFrame;
import org.scijava.widget.UIComponent;


public class LegacyApplicationFrame implements UIComponent<ImageJ>, ApplicationFrame {

	@Override
	public void setLocation(int x, int y) {
		final ImageJ ij = IJ.getInstance();
		if (ij != null) {
			ij.setLocation(x, y);
		}
	}

	@Override
	public int getLocationX() {
		final ImageJ ij = IJ.getInstance();
		if (ij == null) return 0;
		return ij.getX();
	}

	@Override
	public int getLocationY() {
		final ImageJ ij = IJ.getInstance();
		if (ij == null) return 0;
		return ij.getY();
	}

	@Override
	public void activate() {
		setVisible(true);
	}

	@Override
	public void setVisible(boolean visible) {
		final ImageJ ij = IJ.getInstance();
		if (ij != null) {
			ij.setVisible(visible);
		}
	}

	@Override
	public ImageJ getComponent() {
		return IJ.getInstance();
	}

	@Override
	public Class<ImageJ> getComponentType() {
		return ij.ImageJ.class;
	}

}
