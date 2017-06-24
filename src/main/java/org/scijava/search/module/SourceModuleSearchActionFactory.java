
package org.scijava.search.module;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.search.DefaultSearchAction;
import org.scijava.search.SearchAction;
import org.scijava.search.SearchActionFactory;
import org.scijava.search.SearchResult;
import org.scijava.ui.UIService;

@Plugin(type = SearchActionFactory.class)
public class SourceModuleSearchActionFactory implements SearchActionFactory {

	@Parameter
	private UIService uiService;

	@Override
	public boolean supports(final SearchResult result) {
		return result instanceof ModuleSearchResult;
	}

	@Override
	public SearchAction create(final SearchResult result) {
		return new DefaultSearchAction("Source", () -> {
			uiService.showDialog("TODO: source for module: " +
				((ModuleSearchResult) result).info().getTitle());
		});
	}
}
