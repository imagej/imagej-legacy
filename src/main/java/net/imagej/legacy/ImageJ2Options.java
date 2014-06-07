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

import org.scijava.command.Interactive;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.welcome.WelcomeService;
import org.scijava.widget.Button;

/**
 * Allows the setting and persisting of options relevant to ImageJ2, in ImageJ1.
 * Not displayed in the IJ2 UI.
 * 
 * @author Mark Hiner
 */
@Plugin(type = OptionsPlugin.class, visible = false,
	label = "ImageJ2 Options", menu = {
		@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
			mnemonic = MenuConstants.EDIT_MNEMONIC), @Menu(label = "Options"),
		@Menu(label = "ImageJ2") })
public class ImageJ2Options extends OptionsPlugin implements Interactive
{

	// Constants for field lookup

	public static final String USE_SCIFIO = "useSCIFIO";

	// Fields

	/**
	 * If true, SCIFIO will be used during {@code File > Open} IJ1 calls.
	 */
	@Parameter(label = "Use SCIFIO when opening files", callback = "run")
	private Boolean useSCIFIO = true;

	@Parameter(label = "What is ImageJ2?", persist = false, callback = "help")
	private Button help;

	@Parameter
	private DefaultLegacyService legacyService;

	@Parameter(required = false)
	private WelcomeService welcomeService;

	// -- Option accessors --

	public Boolean isUseSCIFIO() {
		return useSCIFIO;
	}

	@SuppressWarnings("unused")
	private void help() {
		if (welcomeService != null) {
			welcomeService.displayWelcome();
		}
	}
}
