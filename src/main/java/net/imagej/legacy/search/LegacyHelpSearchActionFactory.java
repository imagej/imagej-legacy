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

import java.io.IOException;
import java.net.URL;

import net.imagej.legacy.command.LegacyCommand;
import net.imagej.legacy.command.LegacyCommandInfo;
import org.scijava.Priority;
import org.scijava.log.LogService;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.search.DefaultSearchAction;
import org.scijava.search.SearchAction;
import org.scijava.search.SearchActionFactory;
import org.scijava.search.SearchResult;
import org.scijava.search.module.ModuleSearchResult;

/**
 * Search action for viewing the documentation of a {@link LegacyCommand}.
 *
 * @author Gabriel Selzer
 */
@Plugin(type = SearchActionFactory.class, priority = Priority.HIGH)
public class LegacyHelpSearchActionFactory implements SearchActionFactory {

	@Parameter
	private PlatformService platformService;

	@Parameter
	private LogService log;

	@Override
	public boolean supports(final SearchResult result) {
		if (!(result instanceof ModuleSearchResult)) return false;
		return ((ModuleSearchResult) result).info() instanceof LegacyCommandInfo;
	}

	@Override
	public SearchAction create(final SearchResult result) {
		return new DefaultSearchAction("Help", //
			() -> help(((ModuleSearchResult) result)));
	}

	private void help(ModuleSearchResult result) {
		if (!(result.info() instanceof LegacyCommandInfo)) {
			throw new IllegalArgumentException("Not a legacy command: " + result
				.info());
		}
		// Base help url is the imagej documentation
		String url = "https://imagej.net/ij/docs";
		// If the legacy command is in the menus, we can do better by linking
		// directly to it.
		if (result.info().getMenuPath().size() > 0) {
			// Determine menu page
			String page = result.info().getMenuPath().get(0).getName().toLowerCase();
			// Determine anchor
			String anchor = result.info().getMenuPath().get(1).getName()
				.toLowerCase();
			if (anchor.indexOf(' ') == -1) {
				anchor = anchor.replaceAll("\\.\\.\\.", "");
				anchor = "#" + anchor;
			}
			else {
				// Unfortunately, the anchor is hard to determine in this case.
				// The anchor is usually the first word of the command name, UNLESS
				// there
				// is ANOTHER command with that name. In that case, it is the LAST word
				// of the command. But there's not a good way for us to determine
				// whether
				// that other command exists, so we give up and link to the menu
				// instead.
				log.debug("Anchor cannot be obtained for multi-word command " +
					"names. Linking instead to the menu of " + result.info().getName());
				anchor = "";
			}
			// Build URL
			url = url.concat("/menus/" + page + ".html" + anchor);
		}
		try {
			platformService.open(new URL(url));
		}
		catch (IOException exc) {
			log.error(exc);
		}
	}
}
