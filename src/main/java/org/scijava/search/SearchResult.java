package org.scijava.search;

import java.util.Map;

/**
 * Data container for one item of a search result.
 * 
 * @author Curtis Rueden
 */
public interface SearchResult {

	String name();
	String iconPath();
	Map<String, String> properties();
}
