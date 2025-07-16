/*-
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2025 ImageJ2 developers.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.imagej.legacy.IJ1Helper;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.SortByRelevanceComparator;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Creates the list of auto-completion suggestions from functions.html
 * documentation.
 *
 * @author Robert Haase
 */
class MacroAutoCompletionProvider extends DefaultCompletionProvider implements
	ToolTipSupplier
{
	private ModuleService moduleService;
	private MacroExtensionAutoCompletionService macroExtensionAutoCompletionService;

	private static MacroAutoCompletionProvider instance = null;

	private boolean sorted = false;
	private final int maximumSearchResults = 100;

	private MacroAutoCompletionProvider() {
		parseFunctionsHtmlDoc("/doc/ij1macro/functions.html");
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

		sorted = false;
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
						headline = htmlToText(line + ";");
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

	void addModuleCompletions(ModuleService moduleService) {
		if (this.moduleService == moduleService) {
			return;
		}
		sorted = false;
		this.moduleService = moduleService;

		ArrayList<Completion> completions = new ArrayList<>();

		for (ModuleInfo info : moduleService.getModules()) {
			if(info.getMenuPath().getLeaf() != null) {
				String name = info.getMenuPath().getLeaf().getName().trim();
				String headline = "run(\"" + name +"\");";
				String description = "<b>" + headline + "</b><p>" +
						"<a href=\"https://imagej.net/Special:Search/" + name.replace(" ", "%20") + "\">Search imagej wiki for help</a>";

				completions.add(makeListEntry(this, headline, null, description));
			}
		}
		addCompletions(completions);
	}

	public void addMacroExtensionAutoCompletions(MacroExtensionAutoCompletionService macroExtensionAutoCompletionService) {
		if (this.macroExtensionAutoCompletionService != null) {
			return;
		}
		sorted = false;
		this.macroExtensionAutoCompletionService = macroExtensionAutoCompletionService;

		List<BasicCompletion> completions = macroExtensionAutoCompletionService.getCompletions(this);
		List<Completion> completionsCopy = new ArrayList<>();
		for (BasicCompletion completion : completions) {
			completionsCopy.add(completion);
		}
		addCompletions(completionsCopy);

	}

	public void sort() {
		if (!sorted) {
			Collections.sort(completions, new SortByRelevanceComparator());
			sorted = true;
		}
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
			name.compareTo("ext") != 0 && //
			name.compareTo("Math") != 0;
	}

	private String htmlToText(final String text) {
		return text //
			.replace("&quot;", "\"") //
			.replace("&amp;", "&") //
			.replace("<b>", "") //
			.replace("</b>", "") //
			.replace("<i>", "") //
			.replace("</i>", "") //
			.replace("<br>", "\n")
			.replace(")\n;", ");")
				.replace("\n;", "\n")
				.replace("{;", "{")
				.replace("};", "}")
				.replace(") -;", ");");
	}

	private BasicCompletion makeListEntry(
		final MacroAutoCompletionProvider provider, String headline,
		final String name, String description)
	{
		if (!headline.startsWith("run(\"")) {
			final String link = //
					"https://imagej.net/developer/macro/functions.html#" + name;

			description = //
					"<a href=\"" + link + "\">" + headline + "</a><br>" + description;
		}

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

	protected boolean isValidChar(char ch) {
		return Character.isLetterOrDigit(ch) || ch == '_' || ch == '.' || ch == '"';
	}



	/**
	 * Returns a list of <tt>Completion</tt>s in this provider with the
	 * specified input text.
	 *
	 * @param inputText The input text to search for.
	 * @return A list of {@link Completion}s, or <code>null</code> if there
	 *         are no matching <tt>Completion</tt>s.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Completion> getCompletionByInputText(String inputText) {
		inputText = inputText.toLowerCase();

		ArrayList<Completion> result = new ArrayList<Completion>();

		int count = 0;
		int secondaryCount = 0;
		for (Completion completion : completions) {
			String text = completion.getInputText().toLowerCase();
			if (text.contains(inputText)) {
				if (text.startsWith(inputText)) {
					result.add(count, completion);
					count++;
				} else {
					result.add(completion);
					secondaryCount++;
				}
			}
			if (secondaryCount + count > maximumSearchResults) {
				break; // if too many results are found, exit to not annoy the user
			}
		}

		return result;
	}

	private void appendMacroSpecificCompletions(String input, List<Completion> result, JTextComponent comp) {

		List<Completion> completions = new ArrayList<Completion>();
		String lcaseinput = input.toLowerCase();

		String text = null;
		int carPos = 0;
		try {
			text = comp.getDocument().getText(0, comp.getDocument().getLength());
			carPos = comp.getCaretPosition();
		} catch (BadLocationException e) {
			e.printStackTrace();
			return;
		}
		
		int codeLength = text.length();
		text = text + "\n" + IJ1Helper.getAdditionalMacroFunctions();

		List<String> userVariables = new ArrayList<String>();
		List<String> varLines = new ArrayList<String>();
		List<Boolean> globalVarStatus = new ArrayList<Boolean>();
		
		int linecount = 0;
		int charcount = 0;
		String[] textArray = text.split("\n");
		for (String line : textArray){
			if(carPos<charcount || carPos>charcount+line.length() + 1) { // the caret is not in the current line
				String trimmedline = line.trim();
				String lcaseline = trimmedline.toLowerCase();
				if (lcaseline.startsWith("function ")) {
					String command = trimmedline.substring(8).trim().replace("{", "").trim();
					String lcasecommand = command.toLowerCase();
					if (lcasecommand.contains(lcaseinput) && command.matches("[_a-zA-Z]+[_a-zA-Z0-9]*\\(.*\\)")) { // the function name is valid and the statement includes parenthesis
						Boolean isAdditional = !(charcount < codeLength);  // function is outside user code block (in additional functions)
						String description = "<b>" + command + "</b><br>" + findDescription(textArray, linecount, "<i>User defined " + (isAdditional?"additional ":"") + "function" + (isAdditional?"":" as specified in line " + (linecount + 1)) + ".</i>");
						
						completions.add(new BasicCompletion(this, command, null, description));
					}
				}
				if ((lcaseline.contains("=") || trimmedline.startsWith("var ")) && (charcount < codeLength)) { // possible variable assignment (= OR var) AND within user code block (not in additional functions)
					String command = trimmedline;
					if(command.contains("=")) command=command.substring(0, lcaseline.indexOf("=")).trim();
					boolean globalVar = false;
					if(command.startsWith("var ")) { 
						command = command.substring(4).trim().replace(";", ""); // in case of var declaration w/o assignment the trailing semicolon will be removed
						globalVar= true;
					}
					String lcasecommand = command.toLowerCase();
					if (lcasecommand.contains(lcaseinput) && command.matches("[_a-zA-Z]+[_a-zA-Z0-9]*")) { // First character cannot be a digit ([_a-zA-Z]+), while the rest can be any valid character ([_a-zA-Z0-9]*)
						if (!userVariables.contains(command)) {
							userVariables.add(command);
							varLines.add(String.valueOf(linecount + 1));						
							globalVarStatus.add(globalVar);
						} else {
							int index=userVariables.indexOf(command);
							varLines.set(index, varLines.get(index) + ", " + String.valueOf(linecount + 1));
							globalVarStatus.set(index, globalVarStatus.get(index) || globalVar);
						}
					}
				}
			}
			linecount++;
			charcount += line.length() + 1;
		}

		for (int i=0; i<userVariables.size(); i++) {
			boolean manyLines = varLines.get(i).contains(",");
			String description = "User defined " + (globalVarStatus.get(i)? "<i>GLOBAL</i> ":"") + "variable &lt;<b>" + userVariables.get(i) + "</b>&gt; as specified in line"+(manyLines? "s":"")+": " + varLines.get(i);
			completions.add(new BasicCompletion(this, userVariables.get(i), null, description));
		}
		
		Collections.sort(completions, new SortByRelevanceComparator());

		result.addAll(0, completions);
	}

	private String findDescription(String[] textArray, int linecount, String defaultDescription) {
		String resultDescription = "";
		int l = linecount - 1;
		while (l > 0) {
			String lineBefore = textArray[l].trim();
			if (lineBefore.startsWith("//")) {
				resultDescription = lineBefore.substring(2) + "\n" + resultDescription;
			} else {
				break;
			}
			l--;
		}
		l = linecount + 1;
		while (l < textArray.length - 1) {
			String lineAfter = textArray[l].trim();
			if (lineAfter.startsWith("//")) {
				resultDescription = resultDescription + "\n" + lineAfter.substring(2);
			} else {
				break;
			}
			l++;
		}
		if (resultDescription.length() > 0) {
			resultDescription = resultDescription + "<br><br>";
		}
		resultDescription = resultDescription + defaultDescription;

		return resultDescription;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected List<Completion> getCompletionsImpl(JTextComponent comp) {

		List<Completion> retVal = new ArrayList<Completion>();
		String text = getAlreadyEnteredText(comp);

		if (text != null) {
			retVal = getCompletionByInputText(text);
			appendMacroSpecificCompletions(text, retVal, comp);
		}
		return retVal;
	}


	@Override
	public List<Completion> getCompletions(JTextComponent comp) {
		List<Completion> completions = this.getCompletionsImpl(comp);
		return completions;
	}

}
