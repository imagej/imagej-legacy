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

package net.imagej.legacy.ui;

import java.awt.Frame;

import net.imagej.legacy.LegacyService;

import org.scijava.ui.ApplicationFrame;
import org.scijava.widget.UIComponent;

/**
 * {@link LegacyAdapter} between the {@link ApplicationFrame} class and active
 * {@code ij.ImageJ}.
 * 
 * @author Mark Hiner
 */
public class LegacyApplicationFrame extends AbstractLegacyAdapter implements
	UIComponent<Frame>, ApplicationFrame
{

	public LegacyApplicationFrame(final LegacyService legacyService) {
		super(legacyService);
	}

	@Override
	public void setLocation(final int x, final int y) {
		helper().setLocation(x, y);
	}

	@Override
	public int getLocationX() {
		return helper().getX();
	}

	@Override
	public int getLocationY() {
		return helper().getY();
	}

	@Override
	public void activate() {
		setVisible(true);
	}

	@Override
	public void setVisible(final boolean visible) {
		helper().setVisible(visible);
	}

	@Override
	public Frame getComponent() {
		return helper().getIJ();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<Frame> getComponentType() {
		return (Class<Frame>) getComponent().getClass();
	}

}
