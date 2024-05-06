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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.scijava.convert.ConvertService;
import org.scijava.table.Table;

public class TableListWrapper implements List<ij.measure.ResultsTable> {

	private List<Table<?, ?>> tables;
	private final ConvertService convertService;
	private List<ij.measure.ResultsTable> resultsTables;

	public TableListWrapper(final List<Table<?, ?>> tables,
		final ConvertService convertService)
	{
		this.tables = tables;
		this.convertService = convertService;
	}

	/**
	 * Returns the source {@code List<Table<?, ?>>}. This may be out of sync.
	 *
	 * @return The source {@code List<Table<?, ?>>}
	 */
	public List<Table<?, ?>> getSource() {
		return tables;
	}

	/**
	 * Synchronizes {@code this} and the source {@code List<Table<?, ?>>}.
	 */
	public void synchronize() {
		if (resultsTables == null) return;
		final List<Table<?, ?>> updated = new ArrayList<>();
		for (final ij.measure.ResultsTable resultsTable : getResultsTables()) {
			final Table<?, ?> table = convertService.convert(resultsTable,
				Table.class);
			updated.add(table);
		}
		tables = updated;
	}

	/**
	 * Synchronizes and returns the source {@code List<Table<?, ?>>}.
	 *
	 * @return The synchronized source {@code List<Table<?, ?>>}
	 */
	public List<Table<?, ?>> getUpdatedSource() {
		if (resultsTables == null) return tables;
		synchronize();
		return getSource();
	}

	@Override
	public int size() {
		return getResultsTables().size();
	}

	@Override
	public boolean isEmpty() {
		return getResultsTables().isEmpty();
	}

	@Override
	public boolean contains(final Object o) {
		return getResultsTables().contains(o);
	}

	@Override
	public Iterator<ij.measure.ResultsTable> iterator() {
		return getResultsTables().iterator();
	}

	@Override
	public Object[] toArray() {
		return getResultsTables().toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return getResultsTables().toArray(a);
	}

	@Override
	public boolean add(final ij.measure.ResultsTable e) {
		return getResultsTables().add(e);
	}

	@Override
	public boolean remove(final Object o) {
		return getResultsTables().remove(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return getResultsTables().containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends ij.measure.ResultsTable> c) {
		return getResultsTables().addAll(c);
	}

	@Override
	public boolean addAll(final int index,
		final Collection<? extends ij.measure.ResultsTable> c)
	{
		return getResultsTables().addAll(index, c);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return getResultsTables().removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return getResultsTables().retainAll(c);
	}

	@Override
	public void clear() {
		getResultsTables().clear();
	}

	@Override
	public ij.measure.ResultsTable get(final int index) {
		return getResultsTables().get(index);
	}

	@Override
	public ij.measure.ResultsTable set(final int index,
		final ij.measure.ResultsTable element)
	{
		return getResultsTables().set(index, element);
	}

	@Override
	public void add(final int index, final ij.measure.ResultsTable element) {
		getResultsTables().add(index, element);
	}

	@Override
	public ij.measure.ResultsTable remove(final int index) {
		return getResultsTables().remove(index);
	}

	@Override
	public int indexOf(final Object o) {
		return getResultsTables().indexOf(o);
	}

	@Override
	public int lastIndexOf(final Object o) {
		return getResultsTables().lastIndexOf(o);
	}

	@Override
	public ListIterator<ij.measure.ResultsTable> listIterator() {
		return getResultsTables().listIterator();
	}

	@Override
	public ListIterator<ij.measure.ResultsTable> listIterator(final int index) {
		return getResultsTables().listIterator(index);
	}

	@Override
	public List<ij.measure.ResultsTable> subList(final int fromIndex,
		final int toIndex)
	{
		return getResultsTables().subList(fromIndex, toIndex);
	}

	// -- Helper methods --

	private List<ij.measure.ResultsTable> getResultsTables() {
		if (resultsTables == null) createResultsTableList();
		return resultsTables;
	}

	private synchronized void createResultsTableList() {
		if (resultsTables != null) return;
		final List<ij.measure.ResultsTable> rts = new ArrayList<>();
		for (final Table<?, ?> table : tables) {
			final ij.measure.ResultsTable rt = convertService.convert(table,
				ij.measure.ResultsTable.class);
			rts.add(rt);
		}
		resultsTables = rts;
	}
}
