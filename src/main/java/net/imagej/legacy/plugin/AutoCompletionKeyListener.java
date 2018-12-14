package net.imagej.legacy.plugin;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * AutoCompletionKeyListener
 * <p>
 * <p>
 * <p>
 * @author Robert Haase
 * October 2018
 */
class AutoCompletionKeyListener implements KeyListener {

    private final static int MINIMUM_WORD_LENGTH_TO_OPEN_PULLDOWN = 1;

    AutoCompletion ac;
    RSyntaxTextArea textArea;
    ArrayList<Character> disabledChars;

		private CompletionProvider provider;

    public AutoCompletionKeyListener(final AutoCompletion ac,
                                     final RSyntaxTextArea textArea,
                                     final CompletionProvider provider)
    {
        this.ac = ac;
        this.textArea = textArea;
        this.provider = provider;

        disabledChars = new ArrayList<>();
        disabledChars.add(' ');
        disabledChars.add('\n');
        disabledChars.add('\t');
        disabledChars.add(';');
    }

    @Override
    public void keyTyped(final KeyEvent e) {

    }

    @Override
    public void keyPressed(final KeyEvent e) {

    }

    @Override
    public void keyReleased(final KeyEvent e) {
        SwingUtilities.invokeLater(() -> {
            if (disabledChars.contains(e.getKeyChar())) {
                if (!e.isControlDown()) {
                    // the pulldown should not be hidden if CTRL+SPACE are pressed
                    ac.hideChildWindows();
                }
            }
            else if (e.getKeyCode() >= 65 // a
                    && e.getKeyCode() <= 90 // z
            ) {
                if (provider.getAlreadyEnteredText(
                        textArea).length() >= MINIMUM_WORD_LENGTH_TO_OPEN_PULLDOWN &&
                        provider.getCompletions(textArea).size() > 1)
                {
                    ac.doCompletion();
                }
            }
        });
    }
}
