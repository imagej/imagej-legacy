/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2018 Board of Regents of the University of
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;

import java.util.Arrays;

import net.imagej.patcher.LegacyInjector;
import net.imagej.table.BoolTable;
import net.imagej.table.ByteTable;
import net.imagej.table.Column;
import net.imagej.table.DefaultBoolTable;
import net.imagej.table.DefaultByteTable;
import net.imagej.table.DefaultColumn;
import net.imagej.table.DefaultFloatTable;
import net.imagej.table.DefaultGenericTable;
import net.imagej.table.DefaultIntTable;
import net.imagej.table.DefaultLongTable;
import net.imagej.table.DefaultResultsTable;
import net.imagej.table.DefaultShortTable;
import net.imagej.table.DoubleColumn;
import net.imagej.table.FloatTable;
import net.imagej.table.GenericColumn;
import net.imagej.table.GenericTable;
import net.imagej.table.IntTable;
import net.imagej.table.LongTable;
import net.imagej.table.ResultsTable;
import net.imagej.table.ShortTable;
import net.imagej.table.Table;
import net.imglib2.type.numeric.integer.ByteType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests table converters and wrappers.
 *
 * @author Alison Walter
 */
public class ResultsTableConversionTest {

	static {
		LegacyInjector.preinit();
	}

	private ij.measure.ResultsTable table;

	private final String[] headings = { "col 1", "col2", "col-3", "col_4",
		"col 5" };

	private final String[] rowLabels = { "label1", null, null, null, "label5" };

	private final double[][] values = { { 100, -30, 0.125, 3456, -0.5 }, { 0, 0,
		11, 113, -93 }, { 0, 0, 0, 0, 0 }, { 0.0009765625, 85.015625, 0,
			-2.00048828125, 101.5 }, { 0, -10, 0.125, 8, 0 } };

	private final String[][] stringValues = { { null, null, null, null, null }, {
		"hello", null, null, null, null }, { "a", "b", "g", "q", "w" }, { null,
			null, "middle", null, null }, { null, null, null, null, "see ya!" } };

	private Context context;

	private ConvertService convertService;

	@Before
	public void setup() {
		context = new Context(ConvertService.class);
		convertService = context.getService(ConvertService.class);

		table = new ij.measure.ResultsTable();

		for (int i = 0; i < headings.length; i++) {
			for (int j = 0; j < rowLabels.length; j++) {
				if (stringValues[i][j] != null) table.setValue(headings[i], j,
					stringValues[i][j]);
				else table.setValue(headings[i], j, values[i][j]);
				if (i == 0 && rowLabels[j] != null) table.setLabel(rowLabels[j], j);
			}
		}
	}

	@After
	public void teardown() {
		context.dispose();
	}

	// -- Test ij.measure.ResultsTable to net.imagej.table.GenericTable --

	@Test
	public void testColumnWrapperDoubleGet() {
		final Column<Object> c = new ResultsTableColumnWrapper(table, 0);

		// Test get
		for (int i = 0; i < table.size(); i++) {
			final Object o = c.get(i);
			assertTrue(o instanceof Double);
			assertEquals((Double) o, values[0][i], 0);
		}
	}

	@Test
	public void testColumnWrapperStringGet() {
		final Column<Object> c = new ResultsTableColumnWrapper(table, 2);

		for (int i = 0; i < table.size(); i++) {
			final Object o = c.get(i);
			assertTrue(o instanceof String);
			assertEquals(o, stringValues[2][i]);
		}
	}

	@Test
	public void testColumnWrapperMixedGet() {
		final Column<Object> c = new ResultsTableColumnWrapper(table, 3);

		for (int i = 0; i < table.size(); i++) {
			final Object o = c.get(i);
			if (i == 2) {
				assertTrue(o instanceof String);
				assertEquals(o, stringValues[3][i]);
			}
			else {
				assertTrue(o instanceof Double);
				assertEquals((Double) o, values[3][i], 0);
			}
		}
	}

	@Test
	public void testColumnWrapper() {
		final Column<Object> c = new ResultsTableColumnWrapper(table, 4);

		// Test set heading
		assertEquals(c.getHeader(), "col 5");
		c.setHeader("header");
		assertEquals(c.getHeader(), "header");

		// Test indexOf
		assertTrue(c.indexOf(8) == 3);
		assertTrue(c.indexOf("see ya!") == 4);

		// Test add value
		c.add("I'm new!");
		assertEquals("I'm new!", c.get(5));
		c.add(17);
		assertEquals(17, (Double) c.get(6), 0);

		assertEquals(7, c.size());

		// Test lastIndexOf
		c.add(8);
		assertEquals(7, c.lastIndexOf(8));
		assertEquals(3, c.indexOf(8));

		c.add("I'm new!");
		assertEquals(8, c.lastIndexOf("I'm new!"));
		assertEquals(5, c.indexOf("I'm new!"));

		assertEquals(9, c.size());

		// Test set value
		c.set(3, "buh bye");
		assertEquals("buh bye", c.get(3));
		c.set(5, 125);
		assertEquals(125, (Double) c.get(5), 0);

		assertEquals(9, c.size());

		// Test clear
		c.clear();
		for (int i = 0; i < c.size(); i++)
			assertEquals(0, (Double) c.get(i), 0);

		table.setNaNEmptyCells(true);
		c.clear();
		for (int i = 0; i < c.size(); i++)
			assertTrue(((Double) c.get(i)).isNaN());
	}

	@Test
	public void testResultsTableWrapper() {
		final Table<Column<? extends Object>, Object> t = new ResultsTableWrapper(
			table);

		// check table dimensions
		assertEquals(5, t.getColumnCount());
		assertEquals(5, t.getRowCount());

		// check values
		for (int i = 0; i < t.getColumnCount(); i++) {
			assertEquals(headings[i], t.getColumnHeader(i));
			for (int j = 0; j < t.getRowCount(); j++) {
				if (stringValues[i][j] != null) assertEquals(stringValues[i][j], t.get(
					i, j));
				else assertEquals(values[i][j], (Double) t.get(i, j), 0);
			}
		}

		// Test set value
		t.set(0, 0, "Hey there");
		assertEquals("Hey there", t.get(0, 0));
		t.set(0, 1, 25);
		assertEquals(25, (Double) t.get(0, 1), 0);

		// Test remove row
		t.removeRow(2);
		for (int i = 0; i < t.getColumnCount(); i++) {
			if (stringValues[i][3] != null) assertEquals(stringValues[i][3], t.get(i,
				2));
			else assertEquals(values[i][3], (Double) t.get(i, 2), 0);
		}
	}

	@Test
	public void testConvert() {
		final GenericTable t = convertService.convert(table, GenericTable.class);

		// check values
		for (int i = 0; i < t.getColumnCount(); i++) {
			assertEquals(headings[i], t.getColumnHeader(i));
			for (int j = 0; j < t.getRowCount(); j++) {
				if (stringValues[i][j] != null) assertEquals(table.getStringValue(i, j),
					t.get(i, j));
				else assertEquals(table.getValueAsDouble(i, j), (Double) t.get(i, j),
					0);
			}
		}
	}

	@Test
	public void testConverterMatchingToTable() {
		final Converter<?, ?> c = convertService.getHandler(table, Table.class);
		assertTrue(c instanceof ResultsTableToGenericTableConverter);
	}

	@Test
	public void testTableUnwrapping() {
		final Byte[][] data = new Byte[][] { { 10, 20, 30 }, { -100, 0, 100 }, { 3,
			4, 5 }, { -50, -51, -52 } };
		final ByteTable bt = new DefaultByteTable(4, 3);
		populateTable(bt, data);
		final ij.measure.ResultsTable rt = new TableWrapper(bt, convertService);

		final Converter<?, ?> c = convertService.getHandler(rt, Table.class);
		assertTrue(c instanceof TableUnwrapper);
		final Table<?, ?> t = c.convert(rt, Table.class);
		assertEquals(bt, t);
	}

	@Test
	public void testMeasurementTable() {
		final Overlay overlay = new Overlay();
		overlay.add(new Roi(10, 10, 5, 6));
		overlay.add(new Roi(1, 40, 25, 103));
		overlay.add(new Roi(20, 18, 87, 12));
		overlay.add(new OvalRoi(40, 100, 3, 15));

		final ImagePlus imagePlus = IJ.createImage("gradient", "8-bit ramp", 200,
			200, 5);
		final ij.measure.ResultsTable measurements = overlay.measure(imagePlus);

		final Table<?, ?> converted = convertService.convert(measurements,
			Table.class);
		assertTablesEqual(measurements, converted);
	}

	// -- Test net.imagej.table.Table to ij.measure.ResultsTable --

	@Test
	public void testConvertDoubleTable() {
		final Double[][] data = new Double[][] { { 10.5, 20.25, 11.0 }, { -0.125,
			100.25, -20.5 } };
		final ResultsTable dt = new DefaultResultsTable(data.length,
			data[0].length);
		populateTable(dt, data);

		final ij.measure.ResultsTable ijTable = convertService.convert(dt,
			ij.measure.ResultsTable.class);
		assertTablesEqual(dt, ijTable);
	}

	@Test
	public void testConvertLongTable() {
		final Long[][] data = new Long[][] { { Long.MAX_VALUE, Long.MIN_VALUE }, {
			0l, 100l }, { -20010l, 8l }, { 101l, 101l }, { -92l, -8000l } };
		final LongTable lt = new DefaultLongTable(data.length, data[0].length);
		populateTable(lt, data);

		// NB: This conversion is lossy!
		final ij.measure.ResultsTable ijTable = convertService.convert(lt,
			ij.measure.ResultsTable.class);

		int ijColumnCount = 0;
		for (int i = 0; i <= ijTable.getLastColumn(); i++)
			if (ijTable.columnExists(i)) ijColumnCount++;

		assertEquals(lt.getColumnCount(), ijColumnCount);
		assertEquals(lt.getRowCount(), ijTable.size());

		for (int c = 0; c < lt.getColumnCount(); c++) {
			assertEquals(lt.getColumnHeader(c), ijTable.getColumnHeading(c));
			for (int r = 0; r < lt.getRowCount(); r++)
				assertEquals(lt.get(c, r).longValue(), (long) ijTable.getValueAsDouble(
					c, r));
		}
	}

	@Test
	public void testConvertGenericTable() {
		final GenericTable t = createGenericTable();
		final ij.measure.ResultsTable ijTable = convertService.convert(t,
			ij.measure.ResultsTable.class);
		assertTablesEqual(t, ijTable);
	}

	@Test
	public void testConvertGenericTableMutating() {
		final GenericTable t = createGenericTable();
		final ij.measure.ResultsTable ijTable = convertService.convert(t,
			ij.measure.ResultsTable.class);
		assertTablesEqual(t, ijTable);

		ijTable.incrementCounter(); // NB: addValue does not append rows
		ijTable.addValue(2, 18);
		assertEquals(4, t.getRowCount());
		assertEquals(18.0, t.get(2, 3));

		ijTable.addValue("heading 1", "greetings!");
		assertEquals(4, t.getRowCount());
		assertEquals("greetings!", t.get(0, 3));

		ijTable.setValue(1, 1, "farewell");
		assertEquals("farewell", t.get(1, 1));

		ijTable.setValue(3, 0, "dog");
		assertEquals("dog", t.get(3, 0));

		ijTable.addValue(4, -144);
		assertEquals(-144.0, t.get(4, 3));
		assertEquals(ijTable.size(), t.get(4).size());
		for (int i = 0; i < t.getRowCount() - 1; i++)
			assertNull(t.get(4, i));

		ijTable.setValue(4, 2, "kitten");
		assertEquals("kitten", t.get(4, 2));
	}

	@Test
	public void testConverterMatchingToResultsTable() {
		// Supported
		final ByteTable byteTable = new DefaultByteTable();
		assertTrue(convertService.getHandler(byteTable,
			ij.measure.ResultsTable.class) instanceof
			TableToResultsTableConverters.ByteTableToResultsTable);

		final ShortTable shortTable = new DefaultShortTable();
		assertTrue(convertService.getHandler(shortTable,
			ij.measure.ResultsTable.class) instanceof
			TableToResultsTableConverters.ShortTableToResultsTable);

		final IntTable intTable = new DefaultIntTable();
		assertTrue(convertService.getHandler(intTable,
			ij.measure.ResultsTable.class) instanceof
			TableToResultsTableConverters.IntTableToResultsTable);

		final LongTable longTable = new DefaultLongTable();
		assertTrue(convertService.getHandler(longTable,
			ij.measure.ResultsTable.class) instanceof
			TableToResultsTableConverters.LongTableToResultsTable);

		final FloatTable floatTable = new DefaultFloatTable();
		assertTrue(convertService.getHandler(floatTable,
			ij.measure.ResultsTable.class) instanceof
			TableToResultsTableConverters.FloatTableToResultsTable);

		final ResultsTable doubleTable = new DefaultResultsTable();
		assertTrue(convertService.getHandler(doubleTable,
			ij.measure.ResultsTable.class) instanceof
			TableToResultsTableConverters.ResultsTableToResultsTable);

		final GenericTable genericTable = new DefaultGenericTable();
		assertTrue(convertService.getHandler(genericTable,
			ij.measure.ResultsTable.class) instanceof
			TableToResultsTableConverters.GenericTableToResultsTable);

		// Type not supported
		final BoolTable boolTable = new DefaultBoolTable();
		assertNull(convertService.getHandler(boolTable,
			ij.measure.ResultsTable.class));

		// Not supported: Generic table w/ non-String/Double values
		final GenericTable invalidTable = createGenericTable();
		final Column<ByteType> byteTypeColumn = new DefaultColumn<>(ByteType.class);
		byteTypeColumn.addAll(Arrays.asList(new ByteType((byte) 122), new ByteType(
			(byte) -1), new ByteType((byte) -30)));
		invalidTable.add(byteTypeColumn);
		assertNull(convertService.getHandler(invalidTable,
			ij.measure.ResultsTable.class));
	}

	@Test
	public void testDuplicateHeadings() {
		// NB: You can create tables with duplicate heading in IJ1, but it may
		// affect the behavior of the table later on.
		final ByteTable duplicateHeadings = new DefaultByteTable(2, 3);
		duplicateHeadings.setColumnHeader(0, "heading");
		duplicateHeadings.setColumnHeader(1, "heading");
		assertTrue(convertService.getHandler(duplicateHeadings,
			ij.measure.ResultsTable.class) instanceof
			TableToResultsTableConverters.ByteTableToResultsTable);
		final ij.measure.ResultsTable rt = convertService.convert(duplicateHeadings,
			ij.measure.ResultsTable.class);

		assertTablesEqual(duplicateHeadings, rt);
	}

	@Test
	public void testResultsTableUnwrapping() {
		final Table<?, ?> wrapped = new ResultsTableWrapper(table);
		assertTrue(convertService.getHandler(wrapped,
			ij.measure.ResultsTable.class) instanceof ResultsTableUnwrapper);
	}

	// -- Helper methods --

	private void assertTablesEqual(final ij.measure.ResultsTable expected,
		final Table<?, ?> actual)
	{
		final int ijColumnCount = computeColumnCount(expected);
		assertEquals(expected.size(), actual.getRowCount());
		assertEquals(computeColumnCount(expected), actual.getColumnCount());

		final int[] columnsInUse = new int[actual.getColumnCount()];
		for (int i = 0; i < ijColumnCount; i++)
			columnsInUse[i] = expected.getColumnIndex(actual.getColumnHeader(i));

		for (int c = 0; c < actual.getColumnCount(); c++) {
			final int actualIJColumnIndex = columnsInUse[c];
			assertEquals(expected.getColumnHeading(actualIJColumnIndex), actual
				.getColumnHeader(c));
			for (int r = 0; r < actual.getRowCount(); r++) {
				final Object actualValue = actual.get(c, r);
				if (actualValue instanceof String) assertEquals(expected.getStringValue(
					actualIJColumnIndex, r), actualValue);
				else assertEquals(expected.getValueAsDouble(actualIJColumnIndex, r),
					(double) actualValue, 0);
			}
		}
	}

	private void assertTablesEqual(final Table<?, ?> expected,
		final ij.measure.ResultsTable actual)
	{
		assertEquals(expected.getColumnCount(), computeColumnCount(actual));
		assertEquals(expected.getRowCount(), actual.size());

		for (int c = 0; c < expected.getColumnCount(); c++) {
			assertEquals(expected.getColumnHeader(c), actual.getColumnHeading(c));
			for (int r = 0; r < expected.getRowCount(); r++) {
				final Object expectedValue = expected.get(c, r);
				if (expectedValue instanceof String) assertEquals(expectedValue, actual
					.getStringValue(c, r));
				else if (expectedValue instanceof Double ||
					expectedValue instanceof Float) assertEquals(expectedValue, actual
						.getValueAsDouble(c, r));
				else if (expectedValue instanceof Number) assertEquals(
					((Number) expectedValue).longValue(), (long) actual.getValueAsDouble(
						c, r));
				else throw new IllegalArgumentException("Unknown type: " + expectedValue
					.getClass());
			}
		}
	}

	private <T, C extends Column<T>> void populateTable(final Table<C, T> t,
		final T[][] data)
	{
		for (int c = 0; c < data.length; c++) {
			for (int r = 0; r < data[c].length; r++) {
				t.set(c, r, data[c][r]);
			}
		}
	}

	private GenericTable createGenericTable() {
		final Column<String> stringCol = new DefaultColumn<>(String.class,
			"heading 1");
		final Column<String> stringColTwo = new DefaultColumn<>(String.class,
			"heading 2");
		final DoubleColumn doubleCol = new DoubleColumn("heading 3");
		final GenericColumn mixedCol = new GenericColumn("heading 4");

		stringCol.addAll(Arrays.asList("hello", "hi", "hey"));
		stringColTwo.addAll(Arrays.asList("bye", "see ya!", "later"));
		doubleCol.fill(new double[] { 100.125, 0, -30209.25 });
		mixedCol.addAll(Arrays.asList("cats", Byte.MAX_VALUE, 0.25));

		final GenericTable t = new DefaultGenericTable();
		t.add(stringCol);
		t.add(stringColTwo);
		t.add(doubleCol);
		t.add(mixedCol);

		return t;
	}

	private int computeColumnCount(final ij.measure.ResultsTable table) {
		int count = 0;
		for (int i = 0; i <= table.getLastColumn(); i++)
			if (table.columnExists(i)) count++;
		return count;
	}
}
