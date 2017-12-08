package org.scijava.search;

import java.util.List;

public interface SearchListener {
	void searchCompleted(Searcher s, List<SearchResult> results);
}
