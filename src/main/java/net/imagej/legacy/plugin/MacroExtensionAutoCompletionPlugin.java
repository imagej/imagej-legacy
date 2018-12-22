package net.imagej.legacy.plugin;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.scijava.plugin.SciJavaPlugin;

import java.util.List;

/**
 * MacroExtensionAutoCompletionPlugin
 *
 * This interface must be implemented in order to extend the auto completion list of the Script editor.
 * Furthermore, the implementing class must be annotated with @Plugin(type = MacroExtensionAutoCompletionPlugin.class).
 *
 * Author: @haesleinhuepf
 * December 2018
 */

public interface MacroExtensionAutoCompletionPlugin  extends SciJavaPlugin {
    List<BasicCompletion> getCompletions(CompletionProvider completionProvider);
}
