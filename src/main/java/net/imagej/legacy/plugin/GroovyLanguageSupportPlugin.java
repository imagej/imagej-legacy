package net.imagej.legacy.plugin;

import org.fife.ui.autocomplete.CompletionProvider;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.LanguageSupportPlugin;

/**
 * PythonLanguageSupportPlugin
 * <p>
 * <p>
 * <p>
 * @author Robert Haase
 * October 2018
 */
@Plugin(type = LanguageSupportPlugin.class)
public class GroovyLanguageSupportPlugin extends AbstractLanguageSupportPlugin
{
	@Parameter
	private Context context;

	private CompletionProvider provider;

    @Override
    public String getLanguageName() {
        return "groovy";
    }


    @Override
    CompletionProvider getCompletionProvider() {
    	if (provider == null) provider = new ScriptingAutoCompleteProvider(context);
    	return provider;
    }
}
