package net.imagej.legacy.search;

import java.io.IOException;

import net.imagej.legacy.LegacyService;

import org.scijava.Priority;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptInfo;
import org.scijava.search.DefaultSearchAction;
import org.scijava.search.SearchAction;
import org.scijava.search.SearchActionFactory;
import org.scijava.search.SearchResult;
import org.scijava.search.template.TemplateSearchResult;

@Plugin(type = SearchActionFactory.class, priority = Priority.VERY_HIGH)
public class OpenTemplateSearchActionFactory implements SearchActionFactory {
	
	@Parameter
	private Logger log;

	@Parameter
	private LegacyService legacyService;

	@Override
	public boolean supports(final SearchResult result) {
		return result instanceof TemplateSearchResult;
	}

	@Override
	public SearchAction create(final SearchResult result) {
		return new DefaultSearchAction("Open", //
				() -> openTemplate((TemplateSearchResult) result));
	}

	// --- Private methods ---

	private void openTemplate(TemplateSearchResult result) {
		// TextEditor.loadTemplate(result.getURL())
		try {
			legacyService.openScriptInTextEditor(new ScriptInfo(legacyService.context(), result.url(), null));
		} catch (IOException exc) {
			log.error("Unable to open script", exc);
		}
	}
}
