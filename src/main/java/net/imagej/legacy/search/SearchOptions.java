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

package net.imagej.legacy.search;

import org.scijava.ItemVisibility;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;

/**
 * Options relevant to the ImageJ search bar.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = OptionsPlugin.class, label = "Search Options", menu = {
	@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
		mnemonic = MenuConstants.EDIT_MNEMONIC), @Menu(label = "Options"),
	@Menu(label = "Search Bar...") }, attrs = { @Attr(name = "legacy-only") })
public class SearchOptions extends OptionsPlugin {

	// -- Fields --

	@Parameter(label = "Search bar style", choices = { "None", "Mini", "Full" },
		style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
	private String style = "Mini";

	@Parameter(label = "Embed search results in main window")
	private boolean embedded;

	@Parameter(label = "Pressing L focuses the search bar")
	private boolean overrideShortcut = true;

	@Parameter(label = "Close search results when performing default action")
	private boolean closeOnDefaultAction = true;

	@Parameter(label = "Select search results on mouseover")
	private boolean mouseoverEnabled;

	@Parameter(label = "Maximum number of results per category", min = "1")
	private int resultLimit = 8;

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String protip = "<html><b>Search Tips:</b><ul>" +
		"<li>Start a search with <code>!</code> to run a code snippet in any language.</li>" +
		"<li>Start a search with <code>#!js</code> to run a JavaScript code snippet.</li>" +
		"</ul>";

	// -- Option accessors --

	public boolean isSearchBarEnabled() {
		return !"None".equals(style);
	}

	public boolean isSearchBarFull() {
		return "Full".equals(style);
	}

	public boolean isSearchPanelEmbedded() {
		return embedded;
	}

	public boolean isShortcutOverridden() {
		return overrideShortcut;
	}

	public boolean isSearchPanelClosedByDefaultAction() {
		return closeOnDefaultAction;
	}

	public boolean isMouseoverEnabled() {
		return mouseoverEnabled;
	}

	public int getResultLimit() {
		return resultLimit;
	}

	// -- Runnable methods --

	@Override
	public void run() {
		super.run();
		final UIService uiService = getContext().getService(UIService.class);
		if (uiService != null) {
			uiService.showDialog(
				"Please restart ImageJ for the changes to take effect.");
		}
	}
}
