/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2018 Board of Regents of the University of
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

package net.imagej.legacy.plugin;

import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;

/**
 * Creates the list of auto-completion suggestions from functions.html
 * documentation.
 *
 * @author Robert Haase
 */
class MacroAutoCompletionProvider extends DefaultCompletionProvider implements
	ToolTipSupplier
{

	private static MacroAutoCompletionProvider instance = null;

	private MacroAutoCompletionProvider() {
		if (!parseFunctionsHtmlDoc(
			"https://imagej.net/developer/macro/functions.html"))
		{
			parseFunctionsHtmlDoc("/doc/ij1macro/functions.html");
		}
		parseFunctionsHtmlDoc("/doc/ij1macro/functions_extd.html");
	}

	public static synchronized MacroAutoCompletionProvider getInstance() {
		if (instance == null) {
			instance = new MacroAutoCompletionProvider();
		}
		return instance;
	}

	private boolean parseFunctionsHtmlDoc(final String filename) {
		InputStream resourceAsStream;

		try {
			if (filename.startsWith("http")) {
				final URL url = new URL(filename);
				resourceAsStream = url.openStream();
			}
			else {
				resourceAsStream = getClass().getResourceAsStream(filename);
			}
			if (resourceAsStream == null) return false;
			final BufferedReader br = //
				new BufferedReader(new InputStreamReader(resourceAsStream));

			String name = "";
			String headline = "";
			String description = "";
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				line = line.replace("<a name=\"", "<a name=").replace("\"></a>",
					"></a>");
				if (line.contains("<a name=")) {
					if (checkCompletion(headline, name, description)) {
						addCompletion(makeListEntry(this, headline, name, description));
					}
					name = htmlToText(line.split("<a name=")[1].split("></a>")[0]);
					description = "";
					headline = "";
				}
				else {
					if (headline.length() == 0) {
						headline = htmlToText(line);
					}
					else {
						description = description + line + "\n";
					}
				}

			}
			if (checkCompletion(headline, name, description)) {
				addCompletion(makeListEntry(this, headline, name, description));
			}

		}
		catch (final javax.net.ssl.SSLHandshakeException e)
		{
			return false;
		}
		catch (final UnknownHostException e) {
			return false;
		}
		catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean checkCompletion(final String headline, final String name, final String description) {
		return headline.length() > 0 && //
			name.length() > 1 && //
			!name.trim().startsWith("<") && //
			!name.trim().startsWith("-") && //
			name.compareTo("Top") != 0 && //
			name.compareTo("IJ") != 0 && //
			name.compareTo("Stack") != 0 && //
			name.compareTo("Array") != 0 && //
			name.compareTo("file") != 0 && //
			name.compareTo("Fit") != 0 && //
			name.compareTo("List") != 0 && //
			name.compareTo("Overlay") != 0 && //
			name.compareTo("Plot") != 0 && //
			name.compareTo("Roi") != 0 && //
			name.compareTo("String") != 0 && //
			name.compareTo("Table") != 0 && //
			name.compareTo("Ext") != 0 && //
			name.compareTo("ext") != 0 && //
			name.compareTo("alphabar") != 0 && //
			name.compareTo("ext") != 0;
	}

	private String htmlToText(final String text) {
		return text //
			.replace("&quot;", "\"") //
			.replace("<b>", "") //
			.replace("</b>", "") //
			.replace("<i>", "") //
			.replace("</i>", "") //
			.replace("<br>", "");
	}

	private BasicCompletion makeListEntry(
		final MacroAutoCompletionProvider provider, String headline,
		final String name, String description)
	{
		final String link = //
			"https://imagej.net/developer/macro/functions.html#" + name;

		description = //
			"<a href=\"" + link + "\">" + headline + "</a><br>" + description;

		if (headline.trim().endsWith("-")) {
			headline = headline.trim();
			headline = headline.substring(0, headline.length() - 2);
		}

		return new BasicCompletion(provider, headline, null, description);
	}

	/**
	 * Returns the tool tip to display for a mouse event.
	 * <p>
	 * For this method to be called, the <tt>RSyntaxTextArea</tt> must be
	 * registered with the <tt>javax.swing.ToolTipManager</tt> like so:
	 * </p>
	 *
	 * <pre>
	 * ToolTipManager.sharedInstance().registerComponent(textArea);
	 * </pre>
	 *
	 * @param textArea The text area.
	 * @param e The mouse event.
	 * @return The tool tip text, or <code>null</code> if none.
	 */
	@Override
	public String getToolTipText(final RTextArea textArea, final MouseEvent e) {

		String tip = null;

		final List<Completion> completions = //
			getCompletionsAt(textArea, e.getPoint());
		if (completions != null && completions.size() > 0) {
			// Only ever 1 match for us in C...
			final Completion c = completions.get(0);
			tip = c.getToolTipText();
		}

		return tip;
	}

}
