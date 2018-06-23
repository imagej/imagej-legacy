package net.imagej.legacy.plugin;

import org.fife.rsta.ac.AbstractLanguageSupport;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.LanguageSupportPlugin;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.List;
import java.util.Scanner;

/**
 * MacroLanguageSupportPlugin
 *
 * This plugin makes basic auto completion available for IJ macro scripts. It offers
 * all commands and additional help copied from this website:
 *
 * https://imagej.nih.gov/ij/developer/macro/functions.html
 *
 * Author: @haesleinhuepf
 * June 2018
 */
@Plugin(type = LanguageSupportPlugin.class)
public class MacroLanguageSupportPlugin extends AbstractLanguageSupport implements
        LanguageSupportPlugin {

    private static MacroAutoCompletionProvider provider = null;

    @Override
    public String getLanguageName() {
        return "IJ1 Macro";
    }

    @Override
    public void install(RSyntaxTextArea rSyntaxTextArea) {
        AutoCompletion ac = createAutoCompletion(getMacroAutoCompletionProvider());
        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationDelay(100);
        ac.setShowDescWindow(true);
        ac.install(rSyntaxTextArea);
        installImpl(rSyntaxTextArea, ac);

        rSyntaxTextArea.setToolTipSupplier(getMacroAutoCompletionProvider());
    }

    private MacroAutoCompletionProvider getMacroAutoCompletionProvider() {
        if (provider == null) {
            provider = new MacroAutoCompletionProvider();
        }
        return provider;
    }

    private class MacroAutoCompletionProvider extends DefaultCompletionProvider implements ToolTipSupplier {

        public MacroAutoCompletionProvider() {
            System.err.println("Hello world");

            ClassLoader classLoader = getClass().getClassLoader();
            InputStream resourceAsStream = classLoader.getResourceAsStream("/doc/ij1macro/functions.html");

            try {
                BufferedReader br
                        = new BufferedReader(new InputStreamReader(resourceAsStream));

                String name = "";
                String description = "";
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("<a name=\"")) {
                        if (name.length() > 1) {
                            addCompletion(new BasicCompletion(this, name, name, description));
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
                    addCompletion(new BasicCompletion(this, name, name, description));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Returns the tool tip to display for a mouse event.<p>
         *
         * For this method to be called, the <tt>RSyntaxTextArea</tt> must be
         * registered with the <tt>javax.swing.ToolTipManager</tt> like so:
         *
         * <pre>
         * ToolTipManager.sharedInstance().registerComponent(textArea);
         * </pre>
         *
         * @param textArea The text area.
         * @param e The mouse event.
         * @return The tool tip text, or <code>null</code> if none.
         */
        @Override
        public String getToolTipText(RTextArea textArea, MouseEvent e) {

            String tip = null;

            List<Completion> completions = getCompletionsAt(textArea, e.getPoint());
            if (completions!=null && completions.size()>0) {
                // Only ever 1 match for us in C...
                Completion c = completions.get(0);
                tip = c.getToolTipText();
            }

            return tip;

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
