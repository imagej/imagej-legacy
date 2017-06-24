
package org.scijava.search.module;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.search.DefaultSearchAction;
import org.scijava.search.SearchAction;
import org.scijava.search.SearchActionFactory;
import org.scijava.search.SearchResult;
import org.scijava.ui.UIService;

@Plugin(type = SearchActionFactory.class)
public class HelpModuleSearchActionFactory implements SearchActionFactory {

	@Parameter
	private UIService uiService;

	@Override
	public boolean supports(final SearchResult result) {
		return result instanceof ModuleSearchResult;
	}

	@Override
	public SearchAction create(final SearchResult result) {
		return new DefaultSearchAction("Help", () -> {
			uiService.showDialog("TODO: help with module: " +
				((ModuleSearchResult) result).info().getTitle());
		});
	}
}
