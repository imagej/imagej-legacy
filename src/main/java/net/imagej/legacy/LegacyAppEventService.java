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

package net.imagej.legacy;

import java.util.List;

import org.scijava.Priority;
import org.scijava.platform.AppEventService;
import org.scijava.platform.DefaultAppEventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.Service;

/**
 * ImageJ service for handling application-level events via ImageJ 1.x.
 * <p>
 * It offloads the {@link #about()}, {@link #prefs()} and {@link #quit()}
 * operations to the {@link IJ1Helper#appAbout}, {@link IJ1Helper#appPrefs}, and
 * {@link IJ1Helper#appQuit} methods, respectively, so that the patched ImageJ
 * 1.x can take care of them in a mostly backwards compatible way.
 * </p>
 * 
 * @author Curtis Rueden
 */
@Plugin(type = Service.class, priority = Priority.HIGH_PRIORITY)
public final class LegacyAppEventService extends DefaultAppEventService {

	@Parameter
	private PluginService pluginService;

	@Parameter
	private DefaultLegacyService legacyService;

	private AppEventService fallback;
	private boolean initialized;

	// -- AppEventService methods --

	@Override
	public void about() {
		legacyService.getIJ1Helper().appAbout(fallback());
	}

	@Override
	public void prefs() {
		legacyService.getIJ1Helper().appPrefs(fallback());
	}

	@Override
	public void quit() {
		legacyService.getIJ1Helper().appQuit(fallback());
	}

	// -- Helper methods - lazy initialization --

	/** Gets {@link #fallback}, initializing if needed. */
	private AppEventService fallback() {
		if (!initialized) initFallback();
		return fallback;
	}

	/** Initializes {@link #fallback}. */
	private synchronized void initFallback() {
		if (initialized) return;
		final List<Service> services =
			getContext().getServiceIndex().get(AppEventService.class);
		for (final Service service : services) {
			if (service != this) {
				fallback = (AppEventService) service;
				break;
			}
		}
		initialized = true;
	}

}
