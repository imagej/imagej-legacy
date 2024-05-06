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

import ij.measure.ResultsTable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.IntStream;

import org.scijava.table.GenericColumn;

/**
 * Wraps a {@link ResultsTable} column as a {@link GenericColumn}.
 *
 * @author Alison Walter
 */
public class ResultsTableColumnWrapper extends GenericColumn {

	private final ResultsTable table;
	private final int col;

	public ResultsTableColumnWrapper(final ResultsTable table, final int col) {
		this.table = table;
		this.col = col;
	}

	@Override
	public String getHeader() {
		return table.getColumnHeading(col);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setHeader(final String header) {
		table.setHeading(col, header);
	}

	@Override
	public boolean isEmpty() {
		return table.getColumn(col) == null;
	}

	@Override
	public boolean contains(final Object o) {
		if (o instanceof Number) {
			final double value = ((Number) o).doubleValue();
			for (int i = 0; i < table.size(); i++) {
				if (table.getValueAsDouble(col, i) == value) return true;
			}
			return false;
		}
		else if (o instanceof String) {
			final String value = ((String) o);
			for (int i = 0; i < table.size(); i++) {
				if (table.getStringValue(col, i).equals(value)) return true;
			}
			return false;
		}
		return false;
	}

	@Override
	public Iterator<Object> iterator() {
		throw new UnsupportedOperationException("iterator()");
	}

	@Override
	public Object[] toArray() {
		final Object[] values = new Object[table.size()];
		for (int i = 0; i < values.length; i++) {
			if (checkString(i)) values[i] = table.getStringValue(col, i);
			else values[i] = table.getValueAsDouble(col, i);
		}
		return values;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final T[] a) {
		final T[] copy = a.length < table.size() ? (T[]) java.lang.reflect.Array
			.newInstance(a.getClass().getComponentType(), table.size()) : a;

		for (int i = 0; i < table.size(); i++) {
			if (checkString(i)) copy[i] = (T) table.getStringValue(col, i);
			else copy[i] = (T) Double.valueOf(table.getValueAsDouble(col, i));
		}
		if (copy.length > table.size()) copy[table.size()] = null;

		return copy;
	}

	/**
	 * Adds the given object to the end of the column. If the passed object is a
	 * {@code Number} it will be represented as a double.
	 */
	@Override
	public boolean add(final Object e) {
		table.incrementCounter(); // addValue does not increment row count
		if (e instanceof Number) table.addValue(col, ((Number) e).doubleValue());
		else if (e != null) table.addValue(table.getColumnHeading(col), e
			.toString());
		else return false;
		return true;
	}

	@Override
	public boolean remove(final Object o) {
		throw new UnsupportedOperationException("remove(Object)");
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		for (final Object o : c) {
			if (!contains(o)) return false;
		}
		return true;
	}

	@Override
	public boolean addAll(final Collection<? extends Object> c) {
		boolean b = false;
		for (final Object d : c) {
			b = b || add(d);
		}
		return b;
	}

	@Override
	public boolean addAll(final int index, final Collection<? extends Object> c) {
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
		// Determine if empty cells are NaN or 0
		double fill = 0;
		try {
			final Field f = ResultsTable.class.getDeclaredField("NaNEmptyCells");
			f.setAccessible(true);
			final boolean nan = (boolean) f.get(table);
			fill = nan ? Double.NaN : 0;
		}
		catch (final Exception exc) {
			// Keep as zero
		}

		for (int i = 0; i < table.size(); i++) {
			// set strings to "", this must be done first. Since setting the string
			// sets the corresponding double value position to NaN
			table.setValue(col, i, "");
			table.setValue(col, i, fill);
		}
	}

	@Override
	public Object get(final int index) {
		if (checkString(index)) return table.getStringValue(col, index);
		return table.getValueAsDouble(col, index);
	}

	/**
	 * Sets the value at the given index. If {@code element} is a {@code Number}
	 * it will be stored as {@code double}.
	 */
	@Override
	public Object set(final int index, final Object element) {
		// Store the previous value
		Object prev = null;
		if (checkString(index)) prev = table.getStringValue(col, index);
		else prev = table.getValueAsDouble(col, index);

		// Set the new value
		if (element instanceof Number) table.setValue(col, index, ((Number) element)
			.doubleValue());
		else if (element != null) table.setValue(col, index, element.toString());
		else throw new NullPointerException();

		return prev;
	}

	@Override
	public void add(final int index, final Object element) {
		throw new UnsupportedOperationException("add(int, Object)");
	}

	@Override
	public Double remove(final int index) {
		throw new UnsupportedOperationException("remove(int)");
	}

	@Override
	public int indexOf(final Object o) {
		return findInRange(o, IntStream.range(0, table.size()));
	}

	@Override
	public int lastIndexOf(final Object o) {
		return findInRange(o, IntStream.range(0, table.size()).map(i -> table
			.size() - i - 1));
	}

	@Override
	public ListIterator<Object> listIterator() {
		throw new UnsupportedOperationException("listIterator()");
	}

	@Override
	public ListIterator<Object> listIterator(final int index) {
		throw new UnsupportedOperationException("listIterator(int)");
	}

	@Override
	public List<Object> subList(final int fromIndex, final int toIndex) {
		final List<Object> l = new ArrayList<>(toIndex - fromIndex);
		for (int i = fromIndex; i < toIndex; i++) {
			if (checkString(i)) l.add(table.getStringValue(col, i));
			else l.add(table.getValueAsDouble(col, i));
		}
		return l;
	}

	@Override
	public int size() {
		return table.size();
	}

	@Override
	public void setSize(final int size) {
		throw new UnsupportedOperationException("setSize(int)");
	}

	// -- Helper methods --

	/**
	 * Checks if the value at the given row in the column is a String.
	 *
	 * @param row position to check value at in column
	 * @return true if the value at the given location is a String, otherwise
	 *         false
	 */
	private boolean checkString(final int row) {
		final double d = table.getValueAsDouble(col, row);
		final String s = table.getStringValue(col, row);

		// convert d to a string, as ResultsTable would
		String c = "";
		try {
			final Field f = ResultsTable.class.getDeclaredField("decimalPlaces");
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
			c = ResultsTable.d2s(d, ResultsTable.AUTO_FORMAT);
		}

		// Special case for NaN
		if (((Double) d).isNaN() && (s == "" || s == null)) return false;

		return !s.equals(c);
	}

	private int findInRange(final Object o, final IntStream range) {
		return range.filter(i -> cellContains(o, i)).findFirst().orElse(-1);
	}

	private boolean cellContains(final Object o, final int row) {
		if (o instanceof Number) return ((Number) o).doubleValue() == table
			.getValueAsDouble(col, row);
		return o instanceof String && o.equals(table.getStringValue(col, row));
	}
}
