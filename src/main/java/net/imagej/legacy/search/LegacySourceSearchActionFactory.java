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

import net.imagej.legacy.command.LegacyCommandInfo;
import org.scijava.Priority;
import org.scijava.module.ModuleInfo;
import org.scijava.plugin.Plugin;
import org.scijava.search.SearchActionFactory;
import org.scijava.search.SearchResult;
import org.scijava.search.SourceSearchActionFactory;
import org.scijava.search.module.ModuleSearchResult;

/**
 * Search action for viewing the source code of a SciJava module.
 *
 * @author Curtis Rueden
 */
@Plugin(type = SearchActionFactory.class, priority = Priority.HIGH)
public class LegacySourceSearchActionFactory extends SourceSearchActionFactory {

	@Override
	public boolean supports(final SearchResult result) {
		if (!(result instanceof ModuleSearchResult)) return false;
		final ModuleInfo info = ((ModuleSearchResult) result).info();
		final String id = info.getIdentifier();
		return id != null && id.startsWith("legacy:");
	}

	@Override
	protected Class<?> classFromSearchResult(SearchResult result) {
		if (!(result instanceof ModuleSearchResult))
			throw new IllegalArgumentException("Illegal SearchResult: " + result);
		final ModuleInfo info = ((ModuleSearchResult) result).info();
		if (!(info instanceof LegacyCommandInfo))
			throw new IllegalArgumentException("Not a LegacyCommandInfo: " + info);
		return ((LegacyCommandInfo) info).loadLegacyClass();
	}
}
