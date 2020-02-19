/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2020 ImageJ developers.
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

import org.scijava.log.LogService;
import org.scijava.module.ModuleInfo;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.search.DefaultSearchAction;
import org.scijava.search.SearchAction;
import org.scijava.search.SearchActionFactory;
import org.scijava.search.SearchResult;
import org.scijava.search.module.ModuleSearchResult;

/**
 * Search action for getting help on a SciJava module.
 *
 * @author Curtis Rueden
 */
@Plugin(type = SearchActionFactory.class)
public class HelpSearchActionFactory implements SearchActionFactory {

	@Parameter
	private PlatformService platformService;

	@Parameter
	private LogService log;

	@Override
	public boolean supports(final SearchResult result) {
		return result instanceof ModuleSearchResult;
	}

	@Override
	public SearchAction create(final SearchResult result) {
		return new DefaultSearchAction("Help", //
			() -> help(((ModuleSearchResult) result)));
	}

	private void help(final ModuleSearchResult result) {
		// HACK: For now, convert the module title into a wiki URL.
		// In future, we need to add a url field to @Plugin for
		// embedding the URL associated with that specific plugin.
		final ModuleInfo info = result.info();
		final String title = info.getTitle();
		final String url = "https://imagej.net/" + //
			title.replaceAll("[^a-zA-Z0-9_-]", "_");
		try {
			platformService.open(new URL(url));
		}
		catch (final IOException exc) {
			log.error(exc);
		}
	}
}
