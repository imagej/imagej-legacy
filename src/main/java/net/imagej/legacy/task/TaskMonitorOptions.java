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

package net.imagej.legacy.task;

import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.ChoiceWidget;

/**
 * Options relevant to the ImageJ task monitor bar.
 * 
 * @author Curtis Rueden, Nicolas Chiaruttini
 */
@Plugin(type = OptionsPlugin.class, label = "Task Monitor Options", menu = {
	@Menu(label = MenuConstants.EDIT_LABEL, weight = MenuConstants.EDIT_WEIGHT,
		mnemonic = MenuConstants.EDIT_MNEMONIC), @Menu(label = "Options"),
	@Menu(label = "Task Monitor Bar...") }, attrs = { @Attr(name = "legacy-only") })
public class TaskMonitorOptions extends OptionsPlugin {

	// -- Fields --

	@Parameter(label = "Task Monitor bar", choices = { "Frame", "Mini", "Disable" },
		style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
	private String style = "Mini";

	@Parameter(label = "Ask for confirmation before cancelling", choices = { "Enable", "Disable" },
			style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
	private String confirmCancel = "Enable";

	@Parameter(label = "Estimate remaining time", choices = { "Enable", "Disable" },
			style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE)
	private String estimateTime = "Enable";

	// -- Option accessors --

	public boolean isTaskMonitorBarEnabled() {
		return !"Disable".equals(style);
	}

	public boolean isTaskMonitorEstimateTime() {
		return !"Disable".equals(estimateTime);
	}

	// -- Runnable methods --

	@Override
	public void run() {
		super.run();
		final UIService uiService = getContext().getService(UIService.class);
		if (uiService != null) {
			uiService.showDialog("Please restart ImageJ for the changes to take effect.");
		}
	}
}
