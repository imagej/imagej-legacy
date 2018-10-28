package net.imagej.legacy.plugin;

import org.fife.ui.autocomplete.CompletionProvider;
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
        implements LanguageSupportPlugin
{
    @Override
    public String getLanguageName() {
        return "groovy";
    }


    @Override
    CompletionProvider getCompletionProvider() {
        return ScriptingAutoCompleteProvider.getInstance();
    }
}
