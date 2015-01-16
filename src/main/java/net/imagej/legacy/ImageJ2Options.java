/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
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

import org.scijava.ItemVisibility;
import org.scijava.app.AppService;
import org.scijava.command.Interactive;
import org.scijava.display.DisplayService;
import org.scijava.io.IOService;
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
 * @author Curtis Rueden
 */
@Plugin(type = OptionsPlugin.class, label = "ImageJ2 Options", menu = {
	@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
		mnemonic = MenuConstants.EDIT_MNEMONIC), @Menu(label = "Options"),
	@Menu(label = "ImageJ2...") }, attrs = { @Attr(name = "legacy-only") })
public class ImageJ2Options extends OptionsPlugin implements Interactive {

	// -- Fields --

	// TODO: Use <html> and <br> to put the following warning into a single
	// parameter. There seems to be a bug with at the moment, though...

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String warning1 =
		"These options enable beta ImageJ2 functionality.";

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String warning2 =
		"You can turn them on for testing, but they are still buggy,";

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String warning3 =
		"and have not yet been optimized for performance.";

	@Parameter(label = "Enable ImageJ2 data structures",
		description = "<html>Whether to synchronize ImageJ 1.x and ImageJ2 data "
			+ "structures.<br>When enabled, commands that use the ImageJ2 API "
			+ "(net.imagej)<br>will function, but with an impact on performance "
			+ "and stability.", callback = "run")
	private boolean syncEnabled = false;

	/**
	 * If true, the <a href="http://imagej.net/SciJava_Common">SciJava Common</a>
	 * {@link IOService} will be used to handle {@code File > Open} IJ1 calls.
	 * This system leverages the <a href="http://imagej.net/SCIFIO">SCIFIO</a>
	 * library to open image files.
	 */
	@Parameter(
		label = "Use SCIFIO when opening files",
		description = "<html>Whether to use ImageJ2's file I/O mechanism when "
			+ "opening files.<br>Image files will be opened using the SCIFIO library "
			+ "(SCientific Image<br>Format Input and Output), which provides truly "
			+ "extensible support for<br>reading and writing image file formats.",
		callback = "run")
	private boolean sciJavaIO = false;

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
		catch (final MalformedURLException e) {
			e.printStackTrace();
		}
		WELCOME_URL = url;
	}

	// -- Option accessors --

	public boolean isSyncEnabled() {
		return syncEnabled;
	}

	public boolean isSciJavaIO() {
		return sciJavaIO;
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
