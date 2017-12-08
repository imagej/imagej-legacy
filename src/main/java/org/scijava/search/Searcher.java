package org.scijava.search;

import java.util.List;

import org.scijava.plugin.SciJavaPlugin;

/**
 * SciJava plugin type for discovering search results of a particular sort. For
 * example, a {@code ModuleSearch} plugin looks for SciJava modules relevant to
 * the given search string.
 * 
 * @author Curtis Rueden
 */
public interface Searcher extends SciJavaPlugin {

	String title();

	/** Searches for the given text. */
	List<SearchResult> search(String text, boolean fuzzy);

}
