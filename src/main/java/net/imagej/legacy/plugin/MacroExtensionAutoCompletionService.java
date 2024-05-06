/*-
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

package net.imagej.legacy.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.imagej.ImageJService;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

/**
 * This service manages extensions to the auto-complete pull down of the script
 * editor.
 * 
 * @author Robert Haase
 */
@Plugin(type = Service.class)
public class MacroExtensionAutoCompletionService extends
	AbstractPTService<MacroExtensionAutoCompletionPlugin> implements ImageJService
{

	private final HashMap<String, PluginInfo<MacroExtensionAutoCompletionPlugin>> macroExtensionAutoCompletionPlugins =
		new HashMap<>();

	boolean initialized = false;

	public List<BasicCompletion> getCompletions(
		final CompletionProvider completionProvider)
	{
		if (!initialized) initializeService();
		final ArrayList<BasicCompletion> completions = new ArrayList<>();
		for (final String key : macroExtensionAutoCompletionPlugins.keySet()) {
			final PluginInfo<MacroExtensionAutoCompletionPlugin> info =
				macroExtensionAutoCompletionPlugins.get(key);

			final List<BasicCompletion> list = pluginService().createInstance(info)
				.getCompletions(completionProvider);
			completions.addAll(list);

		}
		return completions;
	}

	@Override
	public Class<MacroExtensionAutoCompletionPlugin> getPluginType() {
		return MacroExtensionAutoCompletionPlugin.class;
	}

	private synchronized void initializeService() {
		if (initialized) return;
		for (final PluginInfo<MacroExtensionAutoCompletionPlugin> info : getPlugins()) {
			String name = info.getName();
			if (name == null || name.isEmpty()) {
				name = info.getClassName();
			}
			macroExtensionAutoCompletionPlugins.put(name, info);
		}
		initialized = true;
	}
}
