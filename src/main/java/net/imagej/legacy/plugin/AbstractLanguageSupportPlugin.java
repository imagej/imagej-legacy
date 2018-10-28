package net.imagej.legacy.plugin;

import org.fife.rsta.ac.AbstractLanguageSupport;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.LanguageAwareCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.swing.script.LanguageSupportPlugin;

import java.awt.event.KeyListener;
import java.util.ArrayList;

/**
 * AbstractLanguageSupportPlugin
 * <p>
 * <p>
 * <p>
 * @author Robert Haase
 * October 2018
 */
abstract class AbstractLanguageSupportPlugin  extends AbstractLanguageSupport
        implements LanguageSupportPlugin
{
    @Parameter
    ModuleService moduleService;

    private final static int MINIMUM_WORD_LENGTH_TO_OPEN_PULLDOWN = 1;

    @Override
    public void install(final RSyntaxTextArea rSyntaxTextArea) {
        final AutoCompletion ac = createAutoCompletion(new LanguageAwareCompletionProvider(getCompletionProvider()));
        ac.setAutoActivationDelay(100);
        ac.setAutoActivationEnabled(true);
        ac.setShowDescWindow(true);
        ac.install(rSyntaxTextArea);
        installImpl(rSyntaxTextArea, ac);

        rSyntaxTextArea.addKeyListener(new AutoCompletionKeyListener(ac,
                rSyntaxTextArea));
    }

    abstract CompletionProvider getCompletionProvider();

    @Override
    public void uninstall(final RSyntaxTextArea rSyntaxTextArea) {
        uninstallImpl(rSyntaxTextArea);

        final ArrayList<KeyListener> toRemove = new ArrayList<>();
        for (final KeyListener keyListener : rSyntaxTextArea.getKeyListeners()) {
            if (keyListener instanceof AutoCompletionKeyListener) {
                toRemove.add(keyListener);
            }
        }
        for (final KeyListener keyListener : toRemove) {
            rSyntaxTextArea.removeKeyListener(keyListener);
        }

    }
}
