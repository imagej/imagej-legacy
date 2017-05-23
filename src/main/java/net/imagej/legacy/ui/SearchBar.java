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

package net.imagej.legacy.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.imagej.legacy.LegacyService;
import net.miginfocom.swing.MigLayout;

import org.scijava.Context;
import org.scijava.MenuEntry;
import org.scijava.MenuPath;
import org.scijava.app.AppService;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.util.ClassUtils;
import org.scijava.util.FileUtils;

/**
 * Swing-based search bar for the main ImageJ window.
 *
 * @author Curtis Rueden
 */
public class SearchBar extends JTextField {

	private static final String DEFAULT_MESSAGE = "Click here to search";

	@Parameter
	private Context context;

	@Parameter
	private ModuleService moduleService;

	@Parameter
	private AppService appService;

	private final Window parent;
	private JDialog dialog;
	private CommandFinder commandFinder;

	public SearchBar(final Context context, final Window parent) {
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
						if (commandFinder != null) commandFinder.up();
						break;
					case KeyEvent.VK_DOWN:
						if (commandFinder != null) commandFinder.down();
						break;
					case KeyEvent.VK_TAB:
						if (commandFinder != null) commandFinder.requestFocus();
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

			dialog = new JDialog(parent, "Search Commands");
			final String baseDir = //
				appService.getApp().getBaseDirectory().getAbsolutePath();
			commandFinder = new CommandFinder(baseDir);
			dialog.setContentPane(commandFinder);
			dialog.pack();

			// position below the main ImageJ window
			final int x = parent.getLocation().x;
			final int y = parent.getLocation().y + parent.getHeight() + 1;
			dialog.setLocation(x, y);
		}
		commandFinder.filter(getText());
		if (!dialog.isVisible()) {
			dialog.setFocusableWindowState(false);
			dialog.setVisible(true);
			dialog.setFocusableWindowState(true);
		}
	}

	public void run() {
		ModuleInfo info = null;
		if (commandFinder != null) info = commandFinder.getCommand();
		reset();
		if (info != null) moduleService.run(info, true);
	}

	public void activate() {
		requestFocus();
	}

	public void reset() {
		if (dialog != null) {
			commandFinder = null;
			dialog.dispose();
			dialog = null;
		}
		setText("");
	}

	// -- Helper classes --

	/**
	 * A panel that allows the user to search for SciJava commands.
	 * <p>
	 * This is a fork of the SciJava command finder panel; see
	 * {@link org.scijava.ui.swing.commands.CommandFinderPanel}.
	 * </p>
	 */
	private class CommandFinder extends JPanel {

		protected final JTable commandsList;
		protected final CommandTableModel tableModel;

		private final List<ModuleInfo> commands;

		public CommandFinder(final String baseDir) {
			commands = buildCommands();

			setPreferredSize(new Dimension(800, 600));

			commandsList = new JTable(20, CommandTableModel.COLUMN_COUNT);
			commandsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			commandsList.setRowSelectionAllowed(true);
			commandsList.setColumnSelectionAllowed(false);
			commandsList.setAutoCreateRowSorter(true);

			tableModel = new CommandTableModel(commands, baseDir);
			commandsList.setModel(tableModel);
			tableModel.setColumnWidths(commandsList.getColumnModel());

			final String layout = "fill,wrap 2";
			final String cols = "[pref|fill,grow]";
			final String rows = "[pref|fill,grow]";
			setLayout(new MigLayout(layout, cols, rows));
			add(new JScrollPane(commandsList), "grow,span 2");
		}

		// -- CommandFinder methods --

		/** Updates the list of visible commands. */
		public void filter(final String text) {
			final ModuleInfo selected = commandsList.getSelectedRow() < 0 ? null
				: getCommand();
			int counter = 0, selectedRow = -1;
			final String regex = ".*" + text + ".*";
			final List<ModuleInfo> matches = new ArrayList<>();
			for (final ModuleInfo command : commands) {
				if (!command.getMenuPath().toString().toLowerCase().matches(regex))
					continue; // no match
				matches.add(command);
				if (command == selected) selectedRow = counter;
				counter++;
			}
			tableModel.setData(matches);
			if (selectedRow >= 0) {
				commandsList.setRowSelectionInterval(selectedRow, selectedRow);
			}
		}

		public void up() {
			final int rowCount = commandsList.getModel().getRowCount();
			if (rowCount == 0) return;
			select((commandsList.getSelectedRow() + rowCount - 1) % rowCount);
		}

		public void down() {
			final int rowCount = commandsList.getModel().getRowCount();
			if (rowCount == 0) return;
			select((commandsList.getSelectedRow() + 1) % rowCount);
		}

		/** Gets the currently selected command. */
		public ModuleInfo getCommand() {
			if (tableModel.getRowCount() < 1) return null;
			int selectedRow = commandsList.getSelectedRow();
			if (selectedRow < 0) selectedRow = 0;
			return tableModel.get(commandsList.convertRowIndexToModel(selectedRow));
		}

		// -- Helper methods --

		/** Builds the master list of available commands. */
		private List<ModuleInfo> buildCommands() {
			// Add all SciJava modules.
			final List<ModuleInfo> list = new ArrayList<>();
			list.addAll(moduleService.getModules());

			// Add all ImageJ 1.x commands.
			final Hashtable<String, String> ij1Commands = //
				context.service(LegacyService.class).getIJ1Helper().getCommands();
			for (final String label : ij1Commands.keySet()) {
				final String value = ij1Commands.get(label);
				System.out.println(label + " = " + value);
			}

			Collections.sort(list);
			return list;
		}

		private void select(final int i) {
			commandsList.scrollRectToVisible(commandsList.getCellRect(i, 0, true));
			commandsList.setRowSelectionInterval(i, i);
		}
	}

	protected static class CommandTableModel extends AbstractTableModel {

		public final static int COLUMN_COUNT = 6;

		private final String baseDir;
		private List<ModuleInfo> list;

		public CommandTableModel(final List<ModuleInfo> list,
			final String baseDir)
		{
			this.list = list;
			this.baseDir = baseDir;
		}

		public void setData(final List<ModuleInfo> list) {
			this.list = list;
			fireTableDataChanged();
		}

		public void setColumnWidths(final TableColumnModel columnModel) {
			final int[] widths = { 36, 250, 150, 150, 250, 200 };
			for (int i = 0; i < widths.length; i++) {
				columnModel.getColumn(i).setPreferredWidth(widths[i]);
			}
			final TableColumn iconColumn = columnModel.getColumn(0);
			iconColumn.setMaxWidth(36);
			iconColumn.setCellRenderer(new DefaultTableCellRenderer() {

				@Override
				public Component getTableCellRendererComponent(final JTable table,
					final Object value, final boolean isSelected, final boolean hasFocus,
					final int row, final int column)
				{
					return (Component) value;
				}
			});
		}

		public ModuleInfo get(final int index) {
			return list.get(index);
		}

		@Override
		public int getColumnCount() {
			return COLUMN_COUNT;
		}

		@Override
		public String getColumnName(final int column) {
			if (column == 0) return "Icon";
			if (column == 1) return "Command";
			if (column == 2) return "Menu Path";
			if (column == 3) return "Shortcut";
			if (column == 4) return "Identifier";
			if (column == 5) return "File";
			return null;
		}

		@Override
		public int getRowCount() {
			return list.size();
		}

		@Override
		public Object getValueAt(final int row, final int column) {
			final ModuleInfo info = list.get(row);
			if (column == 0) {
				final String iconPath = info.getIconPath();
				if (iconPath == null || iconPath.isEmpty()) return null;
				final URL iconURL = getClass().getResource(iconPath);
				return iconURL == null ? null : new JLabel(new ImageIcon(iconURL));
			}
			if (column == 1) return info.getTitle();
			if (column == 2) {
				final MenuPath menuPath = info.getMenuPath();
				return menuPath == null ? "" : menuPath.getMenuString(false);
			}
			if (column == 3) {
				final MenuPath menuPath = info.getMenuPath();
				final MenuEntry menuLeaf = menuPath == null ? null : menuPath.getLeaf();
				return menuLeaf == null ? "" : menuLeaf.getAccelerator();
			}
			if (column == 4) return info.getIdentifier();
			if (column == 5) {
				final URL location = ClassUtils.getLocation(info
					.getDelegateClassName());
				final File file = FileUtils.urlToFile(location);
				final String path = file == null ? null : file.getAbsolutePath();
				if (path != null && path.startsWith(baseDir)) {
					if (path.length() == baseDir.length()) return "";
					return path.substring(baseDir.length() + 1);
				}
				return file;
			}
			return null;
		}
	}

}
