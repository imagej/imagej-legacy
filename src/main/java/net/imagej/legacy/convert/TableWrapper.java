/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2024 ImageJ2 developers.
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

package net.imagej.legacy.convert;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.scijava.convert.ConvertService;
import org.scijava.table.Column;
import org.scijava.table.Table;

/**
 * Wraps an {@link Table} as a {@link ij.measure.ResultsTable}.
 *
 * @author Alison Walter
 */
public class TableWrapper extends ij.measure.ResultsTable {

	private final Table<?, ?> source;
	private final ConvertService convert;

	public TableWrapper(final Table<?, ?> source, final ConvertService convert) {
		super();
		for (int r = 0; r < source.getRowCount(); r++)
			super.incrementCounter();

		this.source = source;
		this.convert = convert;
		synchronizeToImageJTable();
	}

	public Table<?, ?> getSource() {
		return source;
	}

	@Override
	public synchronized void incrementCounter() {
		super.incrementCounter();
		source.appendRow();
	}

	@Override
	public synchronized void addColumns() {
		super.addColumns();
		final int numIJColumns = super.getLastColumn() + 1;
		while (numIJColumns != source.getColumnCount())
			source.appendColumn();
	}

	// NB: Technically addValue methods overwrite the value in the last row. In
	// IJ1 it looks like when this is called incrementCounter() is called right
	// before.
	@Override
	public void addValue(final int column, final double value) {
		super.addValue(column, value);

		// NB: In IJ String and double values are in separate data structures,
		// so when String values are set the corresponding position in the double
		// structure is set to NaN. These are all in one structure in ImageJ2,
		// so if the value is NaN do not overwrite the value.
		if (Double.isNaN(value)) return;
		createMissingColumns(column);
		setDoubleValue(column, source.getRowCount() - 1, value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void addLabel(final String columnHeading, final String label) {
		super.addLabel(columnHeading, label);
		source.setRowHeader(source.getRowCount() - 1, label);
	}

	@Override
	public void setLabel(final String label, final int row) {
		super.setLabel(label, row);
		source.setRowHeader(row, label);
	}

	@Override
	public void disableRowLabels() {
		super.disableRowLabels();
		if (source.getRowHeader(source.getRowCount() - 1).equals("Label")) {
			for (int r = 0; r < source.getRowCount(); r++)
				source.setRowHeader(r, null);
		}
	}

	@Override
	public int getFreeColumn(final String heading) {
		final int newColumn = super.getFreeColumn(heading);
		createMissingColumns(newColumn);
		if (newColumn >= 0) source.setColumnHeader(newColumn, heading);
		return newColumn;
	}

	@Override
	public void setValue(final int column, final int row, final double value) {
		super.setValue(column, row, value);

		// NB: In IJ String and double values are in separate data structures,
		// so when String values are set the corresponding position in the double
		// structure is set to NaN. These are all in one structure in ImageJ2,
		// so if the value is NaN do not overwrite the value.
		if (Double.isNaN(value)) return;
		createMissingColumns(column);
		setDoubleValue(column, row, value);
	}

	@Override
	public void setValue(final int column, final int row, final String value) {
		super.setValue(column, row, value);
		createMissingColumns(column);
		setStringValue(column, row, value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setHeading(final int column, final String heading) {
		super.setHeading(column, heading);
		createMissingColumns(column);
		source.setColumnHeader(column, heading);
	}

	@Override
	public void setDefaultHeadings() {
		super.setDefaultHeadings();
		int count = 0;
		while (!getDefaultHeading(count).equals("null")) {
			source.setColumnHeader(count, getDefaultHeading(count));
			count++;
		}
	}

	@Override
	public synchronized void deleteRow(final int rowIndex) {
		super.deleteRow(rowIndex);
		source.removeRow(rowIndex);
	}

	@Override
	public void deleteColumn(final String column) {
		super.deleteColumn(column);
		source.removeColumn(column);
	}

	@Override
	public void renameColumn(final String oldName, final String newName) {
		super.renameColumn(oldName, newName);
		source.get(oldName).setHeader(newName);
	}

	@Override
	public synchronized void reset() {
		super.reset();
		source.clear();
	}

	@Override
	public void update(final int measurements, final ImagePlus imp,
		final Roi roi)
	{
		super.update(measurements, imp, roi);
		synchronizeToIJTable();
	}

	@Override
	public boolean applyMacro(final String macro) {
		final boolean applyMacro = super.applyMacro(macro);
		synchronizeToIJTable();
		return applyMacro;
	}

	// -- Helper methods --

	/**
	 * Synchronizes the {@link ij.measure.ResultsTable} to be the same as the
	 * backing {@link Table}.
	 */
	@SuppressWarnings("deprecation")
	private void synchronizeToImageJTable() {
		for (int c = 0; c < source.getColumnCount(); c++) {
			for (int r = 0; r < source.getRowCount(); r++) {
				final Object value = source.get(c, r);
				if (value instanceof Number) super.setValue(c, r, ((Number) value)
					.doubleValue());
				else if (value instanceof String) super.setValue(c, r, (String) value);
				else throw new IllegalArgumentException("Cannot store type " + value
					.getClass() + " in ij.measure.ResultsTable!");
			}
		}

		// NB: Using setValue(String, int, String) or setValue(String, int, double)
		// does not allow null headings
		for (int i = 0; i < source.getColumnCount(); i++)
			super.setHeading(i, source.getColumnHeader(i));
	}

	/**
	 * Synchronizes the backing {@link Table} to be the same as this
	 * {@link ij.measure.ResultsTable}.
	 */
	private void synchronizeToIJTable() {
		for (int c = 0; c <= getLastColumn(); c++) {
			for (int r = 0; r < size(); r++) {
				if (checkString(c, r)) {
					setStringValue(c, r, getStringValue(c, r));
				}
				else {
					setDoubleValue(c, r, getValueAsDouble(c, r));
				}
			}
		}

		for (int i = 0; i < source.getColumnCount(); i++)
			source.setColumnHeader(i, getColumnHeading(i));
	}

	/**
	 * Attempts to set the given location in the backing {@link Table} to a
	 * {@code double} value. There are several cases:
	 * <ul>
	 * <li>Column type extends Number: the double value is converted to the type
	 * of the column</li>
	 * <li>Column is of type String: the double value is converted to a
	 * String</li>
	 * <li>Column is of type Object: the double value is just put into the
	 * table</li>
	 * </ul>
	 * <p>
	 * If the column satisfies none of the above cases an exception is thrown.
	 * </p>
	 *
	 * @param column The column index of the value
	 * @param row The row index of the value
	 * @param value The value to be set
	 */
	private void setDoubleValue(final int column, final int row,
		final double value)
	{
		final Column<?> col = source.get(column);

		// Set value
		if (Number.class.isAssignableFrom(col.getType())) {
			@SuppressWarnings("unchecked")
			final Column<Number> numberColumn = (Column<Number>) col;
			final Number convertedValue = convert.convert(value, numberColumn
				.getType());
			numberColumn.set(row, convertedValue);
		}
		else if (col.getType() == String.class) {
			@SuppressWarnings("unchecked")
			final Column<String> stringColumn = (Column<String>) col;
			stringColumn.set(row, Double.toString(value));
		}
		else if (col.getType() == Object.class) {
			@SuppressWarnings("unchecked")
			final Column<Object> objectColumn = (Column<Object>) col;
			objectColumn.set(row, value);
		}
		else throw new IllegalArgumentException(
			"Cannot add double to column of type " + col.getType());

		// Set column heading
		col.setHeader(super.getColumnHeading(column));
	}

	/**
	 * Attempts to set the value of the backing {@link Table} at the given row and
	 * column to a {@code String}.
	 * <ul>
	 * <li>Column type is String: sets the value</li>
	 * <li>Column type is Object: sets the value</li>
	 * </ul>
	 * <p>
	 * Occasionally, ImageJ 1.x will call methods which use this method to set
	 * "space holder" values. In these cases for {@code Number} type columns their
	 * values will be NaN or 0, and should be ignored.
	 * </p>
	 * <p>
	 * If none of the above cases are satisfied an exception is thrown.
	 * </p>
	 *
	 * @param column The column index of the value
	 * @param row The row index of the value
	 * @param value The value to be set
	 */
	private void setStringValue(final int column, final int row,
		final String value)
	{
		final Column<?> c = source.get(column);

		// NB: This method gets called from setColumn(...) which attempts to fill
		// in empty rows with "". But before this setValue(int, int, double) is
		// called which sets these to NaN.
		if ((c.getType() == Double.class || c.getType() == Float.class) && c.get(
			row).equals(Double.NaN)) return;
		if (Number.class.isAssignableFrom(c.getType()) && c.get(row).equals(0))
			return;
		if (c.getType() == String.class) {
			@SuppressWarnings("unchecked")
			final Column<String> stringColumn = (Column<String>) c;
			stringColumn.set(row, value);
		}
		else if (c.getType() == Object.class) {
			@SuppressWarnings("unchecked")
			final Column<Object> objectColumn = (Column<Object>) c;
			objectColumn.set(row, value);
		}
		else throw new IllegalArgumentException("Cannot add String to column of " +
			"type " + c.getType());
	}

	/**
	 * Checks if the value at the given position is a String.
	 *
	 * @return true if the value at the given location is a String, otherwise
	 *         false
	 */
	private boolean checkString(final int row, final int col) {
		final double d = getValueAsDouble(col, row);
		final String s = getStringValue(col, row);

		// convert d to a string, as ResultsTable would
		String c = "";
		try {
			final Field f = ij.measure.ResultsTable.class.getDeclaredField(
				"decimalPlaces");
			f.setAccessible(true);
			final short[] dec = (short[]) f.get(this);
			final short places = dec[col];
			if (places == Short.MIN_VALUE) {
				final Method m = super.getClass().getDeclaredMethod("n", double.class);
				m.setAccessible(true);
				c = (String) m.invoke(this, d);
			}
			else {
				c = ResultsTable.d2s(d, dec[col]);
			}
		}
		catch (final Exception exc) {
			// if can't get the decimal places or n(...), call d2s with AUTO_FORMAT
			c = ResultsTable.d2s(d, ij.measure.ResultsTable.AUTO_FORMAT);
		}

		// Special case for NaN
		if (((Double) d).isNaN() && (s == "" || s == null)) return false;

		return !s.equals(c);
	}

	/**
	 * If {@code source} does not have a column at the given index, it creates
	 * columns until there is a column for the given index.
	 * <p>
	 * When values are added or set in {@link ij.measure.ResultsTable}s missing
	 * columns are created as needed.
	 * </p>
	 *
	 * @param index The index to which there must be columns
	 */
	private void createMissingColumns(final int index) {
		if (index < source.getColumnCount()) return;
		while (index != (source.getColumnCount() - 1))
			source.appendColumn();
	}
}
