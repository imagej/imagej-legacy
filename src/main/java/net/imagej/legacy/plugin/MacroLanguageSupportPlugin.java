package net.imagej.legacy.plugin;

import org.fife.rsta.ac.AbstractLanguageSupport;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.script.LanguageSupportPlugin;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MacroLanguageSupportPlugin
 *
 * This plugin makes basic auto completion available for IJ macro scripts. It offers
 * all commands and additional help copied from this website:
 *
 * https://imagej.net/developer/macro/functions.html
 *
 * Author: @haesleinhuepf
 * June 2018
 */
@Plugin(type = LanguageSupportPlugin.class)
public class MacroLanguageSupportPlugin extends AbstractLanguageSupport implements
        LanguageSupportPlugin {


    @Override
    public String getLanguageName() {
        return "IJ1 Macro";
    }

    @Override
    public void install(RSyntaxTextArea rSyntaxTextArea) {
        AutoCompletion ac = createAutoCompletion(MacroAutoCompletionProvider.getInstance());
        ac.setAutoActivationDelay(100);
        ac.setAutoActivationEnabled(true);
        ac.setShowDescWindow(true);
        ac.install(rSyntaxTextArea);
        installImpl(rSyntaxTextArea, ac);

        rSyntaxTextArea.addKeyListener(new MacroAutoCompletionKeyListener(ac, rSyntaxTextArea));

        rSyntaxTextArea.setToolTipSupplier(MacroAutoCompletionProvider.getInstance());
    }


    @Override
    public void uninstall(RSyntaxTextArea rSyntaxTextArea) {
        uninstallImpl(rSyntaxTextArea);

        ArrayList<KeyListener> toRemove = new ArrayList<>();
        for (KeyListener keyListener : rSyntaxTextArea.getKeyListeners()) {
            if (keyListener instanceof MacroAutoCompletionKeyListener) {
                toRemove.add(keyListener);
            }
        }
        for (KeyListener keyListener : toRemove) {
            rSyntaxTextArea.removeKeyListener(keyListener);
        }

    }



    private class MacroAutoCompletionKeyListener implements KeyListener {
        AutoCompletion ac;
        RSyntaxTextArea textArea;
        ArrayList<Character> disabledChars;

        public MacroAutoCompletionKeyListener(AutoCompletion ac, RSyntaxTextArea textArea) {
            this.ac = ac;
            this.textArea = textArea;

            disabledChars = new ArrayList<Character>();
            disabledChars.add(' ');
            disabledChars.add('\n');
            disabledChars.add('\t');
            disabledChars.add(';');
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {

        }

        @Override
        public void keyReleased(KeyEvent e) {
            SwingUtilities.invokeLater(()->{
                if (disabledChars.contains(e.getKeyChar())) {
                    if (!e.isControlDown()) { // the pulldown should not be hidden if CTRL+SPACE are pressed
                        ac.hideChildWindows();
                    }
                } else if (
                             e.getKeyCode() >= 65 // a
                          && e.getKeyCode() <= 90 // z
                        )
                {
                    if (MacroAutoCompletionProvider.getInstance().getAlreadyEnteredText(textArea).length() == 2 &&
                        MacroAutoCompletionProvider.getInstance().getCompletions(textArea).size() != 1) {
                        ac.doCompletion();
                    }
                }
            });
        }
    }


}
