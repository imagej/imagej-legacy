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

package net.imagej.legacy.convert;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.imagej.table.Column;
import net.imagej.table.GenericTable;

import ij.measure.ResultsTable;

/**
 * Wraps a {@link ij.measure.ResultsTable} as a {@link GenericTable}.
 *
 * @author Alison Walter
 */
public class ResultsTableWrapper implements GenericTable {

	private final ij.measure.ResultsTable table;

	public ResultsTableWrapper(final ij.measure.ResultsTable table) {
		this.table = table;
	}

	@Override
	public int getColumnCount() {
		return table.getLastColumn() + 1;
	}

	@Override
	public void setColumnCount(final int colCount) {
		throw new UnsupportedOperationException("setColumnCount(int)");
	}

	@Override
	public Column<? extends Object> get(final String colHeader) {
		return new ResultsTableColumnWrapper(table, table.getColumnIndex(
			colHeader));
	}

	@Override
	public Column<? extends Object> appendColumn() {
		// Determine if empty cells are NaN or 0
		double fill = 0;
		try {
			final Field f = ij.measure.ResultsTable.class.getDeclaredField(
				"NaNEmptyCells");
			f.setAccessible(true);
			final boolean nan = (boolean) f.get(table);
			fill = nan ? Double.NaN : 0;
		}
		catch (final Exception exc) {
			// Keep as zero
		}
		// addValue does not increment the counter (row count) and sets the heading
		// to "---"
		table.addValue(table.getLastColumn() + 1, fill);
		return get(table.getLastColumn());
	}

	@Override
	public Column<? extends Object> appendColumn(final String header) {
		final Column<?> c = appendColumn();
		c.setHeader(header);
		return c;
	}

	@Override
	public List<Column<? extends Object>> appendColumns(final int count) {
		final List<Column<?>> l = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			l.add(appendColumn());
		}
		return l;
	}

	@Override
	public List<Column<? extends Object>> appendColumns(final String... headers) {
		final List<Column<?>> l = new ArrayList<>(headers.length);
		for (int i = 0; i < headers.length; i++) {
			l.add(appendColumn(headers[i]));
		}
		return l;
	}

	@Override
	public Column<? extends Object> insertColumn(final int col) {
		throw new IllegalArgumentException("insertColumn(int)");
	}

	@Override
	public Column<? extends Object> insertColumn(final int col,
		final String header)
	{
		throw new IllegalArgumentException("insertColumn(int, String)");
	}

	@Override
	public List<Column<? extends Object>> insertColumns(final int col,
		final int count)
	{
		throw new IllegalArgumentException("insertColumns(int, int)");
	}

	@Override
	public List<Column<? extends Object>> insertColumns(final int col,
		final String... headers)
	{
		throw new IllegalArgumentException("insertColumns(int, String...)");
	}

	@Override
	public Column<? extends Object> removeColumn(final int col) {
		throw new IllegalArgumentException("removeColumn(int)");
	}

	@Override
	public Column<? extends Object> removeColumn(final String header) {
		throw new IllegalArgumentException("removeColumn(String)");
	}

	@Override
	public List<Column<? extends Object>> removeColumns(final int col,
		final int count)
	{
		throw new IllegalArgumentException("removeColumns(int, int)");
	}

	@Override
	public List<Column<? extends Object>> removeColumns(final String... headers) {
		throw new IllegalArgumentException("removeColumns(String...)");
	}

	@Override
	public int getRowCount() {
		return table.size();
	}

	@Override
	public void setRowCount(final int rowCount) {
		throw new IllegalArgumentException("setRowCount(int)");
	}

	@Override
	public void appendRow() {
		// Determine if empty cells are NaN or 0
		double fill = 0;
		try {
			final Field f = ij.measure.ResultsTable.class.getDeclaredField(
				"NaNEmptyCells");
			f.setAccessible(true);
			final boolean nan = (boolean) f.get(table);
			fill = nan ? Double.NaN : 0;
		}
		catch (final Exception exc) {
			// Keep as zero
		}

		for (int i = 0; i <= table.getLastColumn(); i++) {
			// setValue increments the column whereas addValue does not
			table.setValue(i, table.size(), fill);
		}
	}

	@Override
	public void appendRow(final String header) {
		appendRow(); // incremented row count
		table.setLabel(header, table.size() - 1);
	}

	@Override
	public void appendRows(final int count) {
		for (int i = 0; i < count; i++) {
			appendRow();
		}
	}

	@Override
	public void appendRows(final String... headers) {
		for (int i = 0; i < headers.length; i++) {
			appendRow(headers[i]);
		}
	}

	@Override
	public void insertRow(final int row) {
		throw new UnsupportedOperationException("insertRow(int)");
	}

	@Override
	public void insertRow(final int row, final String header) {
		throw new UnsupportedOperationException("insertRow(int, String)");
	}

	@Override
	public void insertRows(final int row, final int count) {
		throw new UnsupportedOperationException("insertRows(int, int)");
	}

	@Override
	public void insertRows(final int row, final String... headers) {
		throw new UnsupportedOperationException("insertRows(int, String...)");
	}

	@Override
	public void removeRow(final int row) {
		table.deleteRow(row);
	}

	@Override
	public void removeRow(final String header) {
		for (int i = 0; i < table.size(); i++) {
			if (table.getLabel(i).equals(header)) {
				table.deleteRow(i);
				break;
			}
		}
	}

	@Override
	public void removeRows(final int row, final int count) {
		for (int i = 0; i < count; i++) {
			table.deleteRow(i + row);
		}
	}

	@Override
	public void removeRows(final String... headers) {
		for (int i = 0; i < headers.length; i++) {
			removeRow(headers[i]);
		}
	}

	@Override
	public void setDimensions(final int colCount, final int rowCount) {
		throw new UnsupportedOperationException("setDimensions(int, int)");
	}

	@Override
	public String getColumnHeader(final int col) {
		return table.getColumnHeading(col);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setColumnHeader(final int col, final String header) {
		table.setHeading(col, header);
	}

	@Override
	public int getColumnIndex(final String header) {
		return table.getColumnIndex(header);
	}

	@Override
	public String getRowHeader(final int row) {
		return table.getLabel(row);
	}

	@Override
	public void setRowHeader(final int row, final String header) {
		table.setLabel(header, row);
	}

	@Override
	public int getRowIndex(final String header) {
		for (int i = 0; i < table.size(); i++) {
			if (table.getLabel(i).equals(header)) return i;
		}
		return -1;
	}

	/**
	 * Sets the value at the given row column. If the passed value is a
	 * {@code Number} it will be stored as a {@code double}.
	 */
	@Override
	public void set(final int col, final int row, final Object value) {
		if (value instanceof String) table.setValue(col, row, (String) value);
		else if (value instanceof Number) table.setValue(col, row, ((Number) value)
			.doubleValue());
	}

	/**
	 * Sets the value at the given row column. If the passed value is a
	 * {@code Number} it will be stored as a {@code double}.
	 */
	@Override
	public void set(final String colHeader, final int row, final Object value) {
		if (value instanceof String) table.setValue(colHeader, row, (String) value);
		else if (value instanceof Number) table.setValue(colHeader, row,
			((Number) value).doubleValue());
	}

	@Override
	public Object get(final int col, final int row) {
		if (checkString(row, col)) return table.getStringValue(col, row);
		return table.getValueAsDouble(col, row);
	}

	@Override
	public Object get(final String colHeader, final int row) {
		if (checkString(row, table.getColumnIndex(colHeader))) return table
			.getStringValue(colHeader, row);
		return table.getValueAsDouble(table.getColumnIndex(colHeader), row);
	}

	@Override
	public int size() {
		return table.getLastColumn() + 1;
	}

	@Override
	public boolean isEmpty() {
		return table.getCounter() == 0 && table.getLastColumn() == 0;
	}

	@Override
	public boolean contains(final Object column) {
		throw new UnsupportedOperationException("contains(Object)");
	}

	@Override
	public Iterator<Column<? extends Object>> iterator() {
		throw new UnsupportedOperationException("iterator()");
	}

	@Override
	public Object[] toArray() {
		final Object[] o = new Object[table.getLastColumn() + 1];
		for (int i = 0; i <= table.getLastColumn(); i++) {
			o[i] = new ResultsTableColumnWrapper(table, i);
		}
		return o;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> A[] toArray(final A[] a) {
		final A[] copy = a.length < table.getLastColumn() + 1
			? (A[]) java.lang.reflect.Array.newInstance(a.getClass()
				.getComponentType(), table.getLastColumn() + 1) : a;

		for (int i = 0; i < table.getLastColumn() + 1; i++) {
			copy[i] = (A) new ResultsTableColumnWrapper(table, i);
		}
		if (copy.length > table.getLastColumn() + 1) copy[table.getLastColumn() +
			1] = null;

		return copy;
	}

	@Override
	public boolean add(final Column<? extends Object> column) {
		final int colIndex = table.getLastColumn() + 1;
		for (int i = 0; i < column.size(); i++) {
			if (column.get(i) instanceof Number) table.setValue(colIndex, i,
				((Number) column.get(i)).doubleValue());
			else if (column.get(i) != null) table.setValue(colIndex, i, column.get(i).toString());
			else return false;
		}
		return true;
	}

	@Override
	public boolean remove(final Object column) {
		throw new UnsupportedOperationException("remove(Object)");
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		throw new UnsupportedOperationException("containsAll(Collection)");
	}

	@Override
	public boolean addAll(
		final Collection<? extends Column<? extends Object>> c)
	{
		boolean b = false;
		for (final Column<?> col : c) {
			b = b || add(col);
		}
		return b;
	}

	@Override
	public boolean addAll(final int col,
		final Collection<? extends Column<? extends Object>> c)
	{
		throw new UnsupportedOperationException("addAll(int, Collection)");
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException("removeAll(Collection)");
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		throw new UnsupportedOperationException("retainAll(Collection)");
	}

	@Override
	public void clear() {
		table.reset();
	}

	@Override
	public Column<? extends Object> get(final int col) {
		return new ResultsTableColumnWrapper(table, col);
	}

	/**
	 * Returns null, since the previous column needs to be overwritten with the
	 * new column.
	 */
	@Override
	public Column<? extends Object> set(final int col,
		final Column<? extends Object> column)
	{
		final Column<Object> w = new ResultsTableColumnWrapper(table, col);
		for (int i = 0; i < column.size(); i++) {
			w.set(i, column.get(i));
		}
		return null;
	}

	@Override
	public void add(final int col, final Column<? extends Object> column) {
		throw new UnsupportedOperationException("add(int, Column)");
	}

	@Override
	public Column<? extends Object> remove(final int col) {
		throw new UnsupportedOperationException("remove(int)");
	}

	@Override
	public int indexOf(final Object column) {
		throw new UnsupportedOperationException("indexOf(Object)");
	}

	@Override
	public int lastIndexOf(final Object column) {
		throw new UnsupportedOperationException("lastIndexOf(Object)");
	}

	@Override
	public ListIterator<Column<? extends Object>> listIterator() {
		throw new UnsupportedOperationException("listIterator()");
	}

	@Override
	public ListIterator<Column<? extends Object>> listIterator(final int col) {
		throw new UnsupportedOperationException("listIterator(int)");
	}

	@Override
	public List<Column<? extends Object>> subList(final int fromCol,
		final int toCol)
	{
		final List<Column<?>> l = new ArrayList<>(toCol - fromCol);
		for (int i = fromCol; i < toCol; i++) {
			l.add(new ResultsTableColumnWrapper(table, i));
		}
		return l;
	}

// -- Helper methods --

	/**
	 * Checks if the value at the given position is a String.
	 *
	 * @return true if the value at the given location is a String, otherwise
	 *         false
	 */
	private boolean checkString(final int row, final int col) {
		final double d = table.getValueAsDouble(col, row);
		final String s = table.getStringValue(col, row);

		// convert d to a string, as ResultsTable would
		String c = "";
		try {
			final Field f = ij.measure.ResultsTable.class.getDeclaredField(
				"decimalPlaces");
			f.setAccessible(true);
			final short[] dec = (short[]) f.get(table);
			final short places = dec[col];
			if (places == Short.MIN_VALUE) {
				final Method m = table.getClass().getDeclaredMethod("n", double.class);
				m.setAccessible(true);
				c = (String) m.invoke(table, d);
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

}
