package net.imagej.legacy.plugin;

import org.fife.rsta.ac.AbstractLanguageSupport;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.LanguageSupportPlugin;

import javax.swing.*;
import java.io.*;
import java.util.Scanner;

/**
 * MacroLanguageSupportPlugin
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 06 2018
 */
@Plugin(type = LanguageSupportPlugin.class)
public class MacroLanguageSupportPlugin extends AbstractLanguageSupport implements
        LanguageSupportPlugin {

    private static DefaultCompletionProvider provider = null;
    private static ToolTipSupplier toolTipper = null;

    @Override
    public String getLanguageName() {
        System.err.println("helloooo");
        return "IJ1 Macro";
    }

    @Override
    public void install(RSyntaxTextArea rSyntaxTextArea) {
        AutoCompletion ac = createAutoCompletion(getMacroAutoCompletionProvider());
        ac.install(rSyntaxTextArea);
        installImpl(rSyntaxTextArea, ac);

        rSyntaxTextArea.setToolTipSupplier(getMacroToolTopProvider());
    }

    private ToolTipSupplier getMacroToolTopProvider() {
        initialize();
        return toolTipper;
    }

    private CompletionProvider getMacroAutoCompletionProvider() {
        initialize();
        return provider;
    }

    private void initialize() {
        System.err.println("Hello world");
        if (provider == null) {
            provider = new DefaultCompletionProvider();
            //provider.addCompletion(new BasicCompletion(provider, "%c", "char", "Prints a character"));

            ClassLoader classLoader = getClass().getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream("/doc/ij1macro/functions.html");

            try {
                BufferedReader br
                        = new BufferedReader(new InputStreamReader(resourceAsStream));

                String name = "";
                String description = "";
                String line;
                while ((line = br.readLine()) != null) {


                    if (line.startsWith("<a name=\"")) {
                        if (name.length() > 1) {
                            provider.addCompletion(new BasicCompletion(provider, name, name, description));
                        }
                        name = htmlToText(line.
                                replace("<a name=\"", "").
                                replace("\"></a>", ""));
                        description = "";
                    } else {
                        description = description + line + "\n";
                    }

                }
                if (name.length() > 1) {
                    provider.addCompletion(new BasicCompletion(provider, name, name, description));
                }

                } catch (IOException e) {
                    e.printStackTrace();
                } ;


            }
    }

    @Override
    public void uninstall(RSyntaxTextArea rSyntaxTextArea) {
        uninstallImpl(rSyntaxTextArea);

    }

    private String htmlToText(String text) {
        return text.
                replace("&quot;", "\"");
    }

}
