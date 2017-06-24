/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
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

package org.scijava.ui.swing.search;

import ij.gui.Toolbar;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.scijava.Context;
import org.scijava.app.AppService;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.swing.search.SwingSearchPanel;

/**
 * Swing-based {@link JTextField} which functions as an entry point for search.
 * <p>
 * Pops up a {@link SwingSearchPanel} on demand when something is typed.
 * </p>
 *
 * @author Curtis Rueden
 */
public class SwingSearchTextField extends JTextField {

	private static final String DEFAULT_MESSAGE = "Click here to search";

	@Parameter
	private Context context;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private AppService appService;

	private final Window parent;
	private JDialog dialog;
	private SwingSearchPanel searchPanel;

	public SwingSearchTextField(final Context context, final Window parent) {
		super(DEFAULT_MESSAGE, 12);
		this.parent = parent;
		context.inject(this);

		addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				run();
			}

		});
		addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(final KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_UP:
						if (searchPanel != null) searchPanel.up();
						break;
					case KeyEvent.VK_DOWN:
						if (searchPanel != null) searchPanel.down();
						break;
					case KeyEvent.VK_TAB:
						if (searchPanel != null) searchPanel.requestFocus();
						break;
					case KeyEvent.VK_ESCAPE:
						reset();
						break;
				}
			}

		});
		getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(final DocumentEvent e) {
				search();
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				search();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
				search();
			}

		});
		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {
				setText("");
			}

			@Override
			public void focusLost(final FocusEvent e) {
				if (getText().equals("")) reset();
			}

		});
	}

	public void search() {
		if (dialog == null) {
			if (getText().equals("") || getText().equals(DEFAULT_MESSAGE)) {
				// NB: Defer creating a new search dialog until something is typed.
				return;
			}

			dialog = new JDialog(parent, "Quick Search");
			final String baseDir = //
				appService.getApp().getBaseDirectory().getAbsolutePath();
			searchPanel = new SwingSearchPanel(baseDir);
			dialog.setContentPane(searchPanel);
			dialog.pack();

			// position below the main ImageJ window
			final int x = parent.getLocation().x;
			final int y = parent.getLocation().y + parent.getHeight() + 1;
			dialog.setLocation(x, y);
		}
		searchPanel.filter(getText());
		if (!dialog.isVisible()) {
			dialog.setFocusableWindowState(false);
			dialog.setVisible(true);
			dialog.setFocusableWindowState(true);
		}
	}

	public void run() {
		ModuleInfo info = null;
		if (searchPanel != null) info = searchPanel.getCommand();
		reset();
		if (info != null) moduleService.run(info, true);
	}

	public void activate() {
		requestFocus();
	}

	public void reset() {
		if (dialog != null) {
			searchPanel = null;
			dialog.dispose();
			dialog = null;
		}
		if (!getText().isEmpty()) setText("");
		else {
			// lose the focus!
			Toolbar.getInstance().requestFocus();
		}
	}
}
