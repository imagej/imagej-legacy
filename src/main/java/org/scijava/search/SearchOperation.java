
package org.scijava.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;
import org.scijava.thread.ThreadService;

public class SearchOperation {

	/** Delay in milliseconds before invoking the searchers. */
	private static final int DELAY = 200;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private ThreadService threadService;

	@Parameter
	private SearchService ss;
	
	private SearchListener[] listeners;

	private List<SearchAttempt> currentSearches = new ArrayList<>();

	private boolean active = true;

	private String query;
	private boolean fuzzy;
	private long lastSearchTime;
	private long lastModifyTime;

	public SearchOperation(final SearchListener... callbacks) {
		listeners = callbacks;
		threadService.run(() -> {
			while (active) {
				if (lastModifyTime - lastSearchTime > DELAY) {
					// time to start a new search!
					// spawn one new thread per searcher
					currentSearches.forEach(search -> search.invalidate());
					currentSearches.clear();
					for (final Searcher searcher : searchers()) {
						final SearchAttempt search = new SearchAttempt(searcher);
						currentSearches.add(search);
						threadService.run(search);
					}
					lastSearchTime = System.currentTimeMillis();
				}
			}
		});
	}

	// Called from EDT only.
	public void terminate() {
		active = false;
	}
	// Called from EDT only.
	public void search(final String text) {
		query = text;
		lastModifyTime = System.currentTimeMillis();
	}
	// Called from EDT only.
	public void setFuzzy(boolean fuzzy) {
		this.fuzzy = fuzzy;
		lastModifyTime = System.currentTimeMillis();
	}

	private List<Searcher> searchers() {
		return pluginService.createInstancesOfType(Searcher.class);
	}

	private class SearchAttempt implements Callable<List<SearchResult>> {
		private Searcher searcher;
		private boolean valid = true;

		private SearchAttempt(Searcher searcher) {
			this.searcher = searcher;
		}

		public void invalidate() {
			valid = false;
		}

		@Override
		public List<SearchResult> call() throws Exception {
			final List<SearchResult> results = searcher.search(query, fuzzy);
			if (!valid) return results;
			for (final SearchListener l : listeners) {
				l.searchCompleted(searcher, results);
			}
			return results;
		}
	}
}
