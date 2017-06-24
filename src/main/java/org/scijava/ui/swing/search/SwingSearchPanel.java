package org.scijava.ui.swing.search;

import java.awt.Component;
import java.awt.Dimension;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.miginfocom.swing.MigLayout;

import org.scijava.module.ModuleInfo;

/**
 * A panel that allows the user to execute quick searches.
 * 
 * @author Curtis Rueden
 */
public class SwingSearchPanel extends JPanel {

	protected final JTable searchResultsTable;
	protected final SearchTableModel tableModel;

	private final List<ModuleInfo> commands;

	public SwingSearchPanel(final String baseDir) {
		setPreferredSize(new Dimension(800, 300));

		searchResultsTable = new JTable(20, SearchTableModel.COLUMN_COUNT);
		searchResultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		searchResultsTable.setRowSelectionAllowed(true);
		searchResultsTable.setColumnSelectionAllowed(false);
		searchResultsTable.setAutoCreateRowSorter(true);

		tableModel = new SearchTableModel(commands, baseDir);
		searchResultsTable.setModel(tableModel);
		tableModel.setColumnWidths(searchResultsTable.getColumnModel());

		final String layout = "fill,wrap 2";
		final String cols = "[pref|fill,grow]";
		final String rows = "[pref|fill,grow]";
		setLayout(new MigLayout(layout, cols, rows));
		add(new JScrollPane(searchResultsTable), "grow,span 2");
	}

	// -- SearchPanel methods --

	/** Updates the list of visible commands. */
	public void filter(final String text) {
		final ModuleInfo selected = searchResultsTable.getSelectedRow() < 0 ? null
			: getCommand();
		int counter = 0, selectedRow = -1;
		final String regex = ".*" + text + ".*";
		final List<ModuleInfo> matches = new ArrayList<>();
		tableModel.setData(matches);
		if (selectedRow >= 0) {
			searchResultsTable.setRowSelectionInterval(selectedRow, selectedRow);
		}
	}

	public void up() {
		final int rowCount = searchResultsTable.getModel().getRowCount();
		if (rowCount == 0) return;
		select((searchResultsTable.getSelectedRow() + rowCount - 1) % rowCount);
	}

	public void down() {
		final int rowCount = searchResultsTable.getModel().getRowCount();
		if (rowCount == 0) return;
		select((searchResultsTable.getSelectedRow() + 1) % rowCount);
	}

	/** Gets the currently selected command. */
	public ModuleInfo getCommand() {
		if (tableModel.getRowCount() < 1) return null;
		int selectedRow = searchResultsTable.getSelectedRow();
		if (selectedRow < 0) selectedRow = 0;
		return tableModel.get(searchResultsTable.convertRowIndexToModel(selectedRow));
	}

	// -- Helper methods --

	private void select(final int i) {
		searchResultsTable.scrollRectToVisible(searchResultsTable.getCellRect(i, 0, true));
		searchResultsTable.setRowSelectionInterval(i, i);
	}

	// -- Helper classes --

	private static class SearchTableModel extends AbstractTableModel {

		public final static int COLUMN_COUNT = 1;

		private final String baseDir;
		private List<ModuleInfo> list;

		public SearchTableModel(final List<ModuleInfo> list,
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
			if (column == 1) return "Title";
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
			return null;
		}
	}
}

