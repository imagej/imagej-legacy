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

package net.imagej.legacy;

import org.scijava.app.AbstractApp;
import org.scijava.app.App;
import org.scijava.plugin.Plugin;

/**
 * Application metadata and operations for ImageJ 1.x.
 * 
 * @author Curtis Rueden
 * @see org.scijava.app.AppService
 */
@Plugin(type = App.class, name = LegacyImageJApp.NAME)
public class LegacyImageJApp extends AbstractApp {

	public static final String NAME = "ImageJ1";

	private LegacyService legacyService;
	private IJ1Helper ij1Helper;

	@Override
	public String getGroupId() {
		return "net.imagej";
	}

	@Override
	public String getArtifactId() {
		return "ij";
	}

	@Override
	public String getVersion() {
		initFields();
		return legacyService == null ? //
			super.getVersion() : legacyService.getVersion();
	}

	// -- AppEventService methods --

	@Override
	public void about() {
		initFields();
		if (ij1Helper != null) ij1Helper.appAbout();
	}

	@Override
	public void prefs() {
		initFields();
		if (ij1Helper != null) ij1Helper.appPrefs();
	}

	@Override
	public void quit() {
		initFields();
		if (ij1Helper != null) ij1Helper.appQuit();
	}

	// -- Helper methods --

	/**
	 * Populates the {@link #legacyService} and {@link #ij1Helper} fields as
	 * appropriate.
	 */
	private void initFields() {
		if (ij1Helper != null) return; // already initialized
		legacyService = context().getService(LegacyService.class);
		if (legacyService == null) return; // no legacy service
		ij1Helper = legacyService.getIJ1Helper();
	}
}
