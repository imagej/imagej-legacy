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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.ByteTable;
import org.scijava.table.Column;
import org.scijava.table.DoubleTable;
import org.scijava.table.FloatTable;
import org.scijava.table.GenericTable;
import org.scijava.table.IntTable;
import org.scijava.table.LongTable;
import org.scijava.table.ShortTable;
import org.scijava.table.Table;

/**
 * Converters which convert {@link Table} to {@link ij.measure.ResultsTable}.
 *
 * @author Alison Walter
 */
public final class TableToResultsTableConverters {

	private TableToResultsTableConverters() {
		// Prevent instantiation of base class
	}

	/**
	 * Abstract base class for converting {@link Table} to
	 * {@link ij.measure.ResultsTable}.
	 * <p>
	 * Converters which extend this class will return {@code null} if the given
	 * {@link Table} contains duplicate column headings.
	 * </p>
	 *
	 * @param <A> Type of {@link Table} being converted
	 */
	public static abstract class AbstractTableToResultsTableConverter<A extends Table<?, ?>>
		extends AbstractConverter<A, ij.measure.ResultsTable>
	{

		@Parameter
		private ConvertService convert;

		@Parameter
		private LogService log;

		@Override
		public Class<ij.measure.ResultsTable> getOutputType() {
			return ij.measure.ResultsTable.class;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T convert(final Object src, final Class<T> dest) {
			if (!Table.class.isInstance(src)) throw new IllegalArgumentException(
				"Cannot convert " + src.getClass() + " to ij.measure.ResultsTable");

			if (containsDuplicateHeadings((Table<?, ?>) src)) log.warn(
				"Table has duplicate column headings.");

			return (T) new TableWrapper((Table<?, ?>) src, convert);
		}

		private boolean containsDuplicateHeadings(final Table<?, ?> table) {
			final List<String> headings = new ArrayList<>();
			for (int c = 0; c < table.getColumnCount(); c++) {
				final String heading = table.getColumnHeader(c);
				if (heading == null || heading.isEmpty()) continue;
				if (headings.contains(heading)) return true;
				headings.add(heading);
			}
			return false;
		}
	}

	/** Converts a {@link ByteTable} to a {@link ij.measure.ResultsTable}. */
	@Plugin(type = Converter.class)
	public static final class ByteTableToResultsTable extends
		AbstractTableToResultsTableConverter<ByteTable>
	{

		@Override
		public Class<ByteTable> getInputType() {
			return ByteTable.class;
		}
	}

	/** Converts a {@link FloatTable} to a {@link ij.measure.ResultsTable}. */
	@Plugin(type = Converter.class)
	public static final class FloatTableToResultsTable extends
		AbstractTableToResultsTableConverter<FloatTable>
	{

		@Override
		public Class<FloatTable> getInputType() {
			return FloatTable.class;
		}
	}

	/**
	 * Converts a {@link GenericTable} to a {@link ij.measure.ResultsTable}. This
	 * converter will only match if all the {@link Column}s in the table are
	 * supported types.
	 */
	@Plugin(type = Converter.class)
	public static final class GenericTableToResultsTable extends
		AbstractTableToResultsTableConverter<GenericTable>
	{

		@Override
		public boolean canConvert(final Object src, final Type dest) {
			return super.canConvert(src, dest) && supportedColumnTypes(
				(GenericTable) src);
		}

		@Override
		public boolean canConvert(final Object src, final Class<?> dest) {
			return super.canConvert(src, dest) && supportedColumnTypes(
				(GenericTable) src);
		}

		@Override
		public Class<GenericTable> getInputType() {
			return GenericTable.class;
		}

		private boolean supportedColumnTypes(final GenericTable table) {
			for (int c = 0; c < table.getColumnCount(); c++) {
				final Column<?> col = table.get(c);
				if (Number.class.isAssignableFrom(col.getType()) || col
					.getType() == String.class || col.getType() == Object.class) continue;
				return false;
			}
			return true;
		}
	}

	/** Converts a {@link IntTable} to a {@link ij.measure.ResultsTable}. */
	@Plugin(type = Converter.class)
	public static final class IntTableToResultsTable extends
		AbstractTableToResultsTableConverter<IntTable>
	{

		@Override
		public Class<IntTable> getInputType() {
			return IntTable.class;
		}
	}

	/** Converts a {@link LongTable} to a {@link ij.measure.ResultsTable}. */
	@Plugin(type = Converter.class)
	public static final class LongTableToResultsTable extends
		AbstractTableToResultsTableConverter<LongTable>
	{

		@Override
		public Class<LongTable> getInputType() {
			return LongTable.class;
		}
	}

	/** Converts a {@link DoubleTable} to a {@link ij.measure.ResultsTable}. */
	@Plugin(type = Converter.class)
	public static final class ResultsTableToResultsTable extends
		AbstractTableToResultsTableConverter<DoubleTable>
	{

		@Override
		public Class<DoubleTable> getInputType() {
			return DoubleTable.class;
		}
	}

	/** Converts a {@link ShortTable} to a {@link ij.measure.ResultsTable}. */
	@Plugin(type = Converter.class)
	public static final class ShortTableToResultsTable extends
		AbstractTableToResultsTableConverter<ShortTable>
	{

		@Override
		public Class<ShortTable> getInputType() {
			return ShortTable.class;
		}
	}

}
