
package org.scijava.search;

import org.scijava.plugin.FactoryPlugin;

public interface SearchActionFactory extends
	FactoryPlugin<SearchResult, SearchAction>
{

	@Override
	default Class<SearchResult> getType() {
		return SearchResult.class;
	}
}
