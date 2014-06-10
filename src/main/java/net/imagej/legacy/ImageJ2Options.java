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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.scijava.app.AppService;
import org.scijava.command.Interactive;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsPlugin;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.text.TextService;
import org.scijava.ui.DialogPrompt.MessageType;
import org.scijava.ui.UIService;
import org.scijava.welcome.WelcomeService;
import org.scijava.widget.Button;

/**
 * Allows the setting and persisting of options relevant to ImageJ2, in ImageJ1.
 * Not displayed in the IJ2 UI.
 * 
 * @author Mark Hiner
 */
@Plugin(type = OptionsPlugin.class,
	label = "ImageJ2 Options", menu = {
		@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
			mnemonic = MenuConstants.EDIT_MNEMONIC), @Menu(label = "Options"),
		@Menu(label = "ImageJ2") }, attrs = { @Attr(name = "legacy-only") })
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

	@Parameter(required = false)
	private AppService appService;

	@Parameter(required = false)
	private PlatformService platformService;

	@Parameter(required = false)
	private DisplayService displayService;

	@Parameter(required = false)
	private TextService textService;

	@Parameter(required = false)
	private UIService uiService;

	@Parameter(required = false)
	private LogService log;

	private final static URL WELCOME_URL;

	static {
		URL url = null;
		try {
			url = new URL("https://github.com/imagej/imagej/blob/master/WELCOME.md#welcome-to-imagej2");
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		WELCOME_URL = url;
	}

	// -- Option accessors --

	public Boolean isUseSCIFIO() {
		return useSCIFIO;
	}

	@SuppressWarnings("unused")
	private void help() {
		if (welcomeService != null) {
			welcomeService.displayWelcome();
			return;
		}
		if (appService != null && textService != null && displayService != null) {
			final File baseDir = appService.getApp().getBaseDirectory();
			final File welcomeFile = new File(baseDir, "WELCOME.md");
			if (welcomeFile.exists()) try {
				final String welcomeText = textService.asHTML(welcomeFile);
				displayService.createDisplay(welcomeText);
				return;
			}
			catch (final IOException e) {
				if (log != null) {
					log.error(e);
				}
				else {
					e.printStackTrace();
				}
			}
		}
		// if local options fail, try the web browser
		if (platformService != null && WELCOME_URL != null) {
			try {
				platformService.open(WELCOME_URL);
				return;
			}
			catch (final IOException e) {
				if (log != null) {
					log.error(e);
				}
				else {
					e.printStackTrace();
				}
			}
		}
		final String message =
			"No appropriate service found to display the message";
		if (uiService != null) {
			uiService.showDialog(message, MessageType.ERROR_MESSAGE);
			return;
		}
		if (log != null) {
			log.error(message);
		}
		else {
			System.err.println(message);
		}
	}
}
