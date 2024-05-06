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

import java.awt.Panel;

import net.imagej.legacy.LegacyService;

import org.scijava.ui.StatusBar;
import org.scijava.widget.UIComponent;

/**
 * Adapter {@link StatusBar} implementation that delegates to legacy ImageJ
 * methods.
 * 
 * @author Mark Hiner
 */
public class LegacyStatusBar extends AbstractLegacyAdapter implements
	UIComponent<Panel>, StatusBar
{

	public LegacyStatusBar(final LegacyService legacyService) {
		super(legacyService);
	}

	@Override
	public void setStatus(final String message) {
		if (dummy()) return;
		boolean processing = getLegacyService().setProcessingEvents(true);
		// if we are already in the middle of processing events, then we must have
		// gotten here from an event that originated in the LegacyStatusBar. So,
		// return, knowing that the value will eventually be restored by another
		// finally block earlier in this stack trace.
		if (processing) return;
		try {
			helper().setStatus(message);
		} finally {
			getLegacyService().setProcessingEvents(processing);
		}
	}

	@Override
	public void setProgress(final int val, final int max) {
		if (dummy()) return;
		boolean processing = getLegacyService().setProcessingEvents(true);
		// if we are already in the middle of processing events, then we must have
		// gotten here from an event that originated in the LegacyStatusBar. So,
		// return, knowing that the value will eventually be restored by another
		// finally block earlier in this stack trace.
		if (processing) return;
		try {
			helper().setProgress(val, max + 1);
		} finally {
			getLegacyService().setProcessingEvents(processing);
		}
	}

	@Override
	public Panel getComponent() {
		return dummy() ? null : helper().getStatusBar();
	}

	@Override
	public Class<Panel> getComponentType() {
		return Panel.class;
	}

}
