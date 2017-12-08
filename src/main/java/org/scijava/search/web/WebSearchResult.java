package org.scijava.search.web;

import org.scijava.search.SearchResult;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a typical web search result being represented
 * by a name/title of a website, an image (icon), the url of the website
 * and some text from the website as preview of its content.
 *
 * Author: Robert Haase (http://haesleinhuepf.net) at MPI CBG (http://mpi-cbg.de)
 * December 2017
 */
public class WebSearchResult implements SearchResult
{
  private final String details;
  String name;
  String iconPath;
  String url;
  public WebSearchResult(String name, String iconPath, String url, String details) {
    this.name = name;
    this.iconPath = iconPath;
    this.url = url;
    this.details = details;
  }

  @Override public String name()
  {
    return name;
  }

  @Override public String iconPath()
  {
    return iconPath;
  }

  @Override public Map<String, String> properties()
  {
    HashMap<String, String> properties = new HashMap<>();
    properties.put("name", name);
    properties.put("iconpath", iconPath);
    properties.put("url", url);
    properties.put("details", details);
    return properties;
  }
}
