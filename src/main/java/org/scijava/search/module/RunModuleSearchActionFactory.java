
package org.scijava.search.module;

import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.search.DefaultSearchAction;
import org.scijava.search.SearchAction;
import org.scijava.search.SearchActionFactory;
import org.scijava.search.SearchResult;

@Plugin(type = SearchActionFactory.class)
public class RunModuleSearchActionFactory implements SearchActionFactory {

	@Parameter
	private ModuleService moduleService;

	@Override
	public boolean supports(final SearchResult result) {
		return result instanceof ModuleSearchResult;
	}

	@Override
	public SearchAction create(final SearchResult result) {
		return new DefaultSearchAction("Run", () -> {
			moduleService.run(((ModuleSearchResult) result).info(), true);
		});
	}
}
