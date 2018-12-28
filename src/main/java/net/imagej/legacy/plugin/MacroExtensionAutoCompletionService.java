package net.imagej.legacy.plugin;

import net.imagej.ImageJService;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.scijava.plugin.AbstractPTService;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * MacroExtensionAutoCompletionService
 *
 * This service manages extensions to the auto-complete pull down of the script editor.
 *
 * Author: @haesleinhuepf
 * December 2018
 */


@Plugin(type = Service.class)
public class MacroExtensionAutoCompletionService  extends AbstractPTService<MacroExtensionAutoCompletionPlugin> implements ImageJService {

    private HashMap<String, PluginInfo<MacroExtensionAutoCompletionPlugin>> macroExtensionAutoCompletionPlugins = new HashMap<>();

    boolean initialized = false;

    private void initializeService() {
        if (initialized) {
            return;
        }
        for (final PluginInfo<MacroExtensionAutoCompletionPlugin> info : getPlugins()) {
            String name = info.getName();
            if (name == null || name.isEmpty()) {
                name = info.getClassName();
            }
            macroExtensionAutoCompletionPlugins.put(name, info);
        }
        initialized = true;
    }

    public List<BasicCompletion> getCompletions(CompletionProvider completionProvider) {
        initializeService();
        ArrayList<BasicCompletion> completions = new ArrayList<BasicCompletion>();
        System.out.println("Completions search");
        for (String key : macroExtensionAutoCompletionPlugins.keySet()) {
            System.out.println("macroext " + key);
            PluginInfo<MacroExtensionAutoCompletionPlugin> info = macroExtensionAutoCompletionPlugins.get(key);

            List<BasicCompletion> list = pluginService().createInstance(info).getCompletions(completionProvider);
            completions.addAll(list);

        }
        return completions;
    }

    @Override
    public Class<MacroExtensionAutoCompletionPlugin> getPluginType() {
        return MacroExtensionAutoCompletionPlugin.class;
    }
}
