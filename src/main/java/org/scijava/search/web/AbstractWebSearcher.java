package org.scijava.search.web;

import org.scijava.search.SearchResult;
import org.scijava.search.Searcher;

import java.util.ArrayList;

/**
 * The AbstractWebSearcher contains convenience function to manage
 * search results of all Searchers browing the web.
 *
 *
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */
public abstract class AbstractWebSearcher implements Searcher
{
  private String title;
  private ArrayList<SearchResult> searchResults = new ArrayList<SearchResult>();

  /**
   *
   * @param title Name of the search engine
   */
  public AbstractWebSearcher(String title) {
    this.title = title;
  }

  @Override public String title()
  {
    return title;
  }

  /**
   *
   * @param name Resulting website title / name
   * @param iconPath path to an image representing the results
   * @param url URL of the found website
   * @param details some text from the website representing its content
   */
  protected void addResult(String name, String iconPath, String url, String details) {
    searchResults.add(new WebSearchResult(name, iconPath, url, details));
  }

  public ArrayList<SearchResult> getSearchResults()
  {
    return searchResults;
  }


}
