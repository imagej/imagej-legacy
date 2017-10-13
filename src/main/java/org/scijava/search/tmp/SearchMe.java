/*
 * #%L
 * SciJava Common shared library for SciJava software.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.search.tmp;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Test UI for search.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Command.class)
public class SearchMe implements Command {

	@Override
	public void run() {
		JFrame f = new JFrame();
		JPanel p = new JPanel();
		f.setContentPane(p);
		final JTextField textField = new JTextField();
		textField.getDocument().addDocumentListener(new DocumentListener() {

			public void popup() {
				final JPopupMenu menu = new JPopupMenu("Hello");
				menu.add(new JLabel("<html><span style=\"color: #000000; background: #ddffff\">Animals</span>"));
				menu.add(new JMenuItem("Dog"));
				menu.add(new JMenuItem("Cat"));
				menu.add(new JLabel("<html><span style=\"color: #000000; background: #ddffff\">Plants</span>"));
				menu.add(new JMenuItem("Tree"));
				menu.add(new JMenuItem("Shrub"));

				menu.show(textField, 0, textField.getHeight());

//				final JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
//				pane.setLeftComponent(menu);
//				pane.setRightComponent(new JTextArea("Lorem ipsum Lorem ipsum Lorem ipsum\nLorem ipsum Lorem ipsum Lorem ipsum\nLorem ipsum Lorem ipsum Lorem ipsum\nLorem ipsum Lorem ipsum Lorem ipsum\nLorem ipsum Lorem ipsum Lorem ipsum\n"));
//
//				final JPopupMenu outer = new JPopupMenu("Bye");
//				outer.add(pane);
//				outer.show(textField, 0, textField.getHeight());
			}

			@Override public void insertUpdate(DocumentEvent e) { popup(); }
			@Override public void removeUpdate(DocumentEvent e) { popup(); }
			@Override public void changedUpdate(DocumentEvent e) { popup(); }
		});
		p.setLayout(new BorderLayout());
		p.add(textField, BorderLayout.CENTER);
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
	}
	
	public static void main(String... args) {
		Context ctx = new Context();
		ctx.service(UIService.class).showUI();
		ctx.service(CommandService.class).run(SearchMe.class, true);
	}
}
