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

package net.imagej.legacy.convert.roi.line;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ij.gui.Arrow;

import net.imagej.legacy.convert.roi.MaskPredicateUnwrappers.WrapperToWritableLine;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToLineConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritableLine;
import net.imglib2.roi.geom.real.Line;
import net.imglib2.roi.geom.real.WritableLine;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting between {@link ij.gui.Line Line} and {@link Line}.
 *
 * @author Alison Walter
 */
public class LineConversionTest {

	private static ij.gui.Line ijLine;
	private static WritableLine ilLine;
	private static Line ijwrap;
	private static ij.gui.Line ilwrap;
	private ConvertService convertService;

	@BeforeClass
	public static void initialize() {
		ijLine = new ij.gui.Line(10.5, 20, 120.5, 150);
		ilLine = new DefaultWritableLine(new double[] { 10.5, 20 }, new double[] {
			120.5, 150 }, false);
		ijwrap = new IJLineWrapper(ijLine);
		ilwrap = new LineWrapper(ilLine);
	}

	@Before
	public void setup() {
		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
		ij.gui.Line.setWidth(1);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- ij.gui.Line to Line tests --

	@Test
	public void testIJLineToLineConverterMatching() {
		// ij.gui.Line to Line (should wrap)
		final Converter<?, ?> ijLineToLine = convertService.getHandler(ijLine,
			Line.class);
		assertTrue(ijLineToLine instanceof IJLineToLineConverter);

		// wrapped Line to Line (should unwrap)
		final Converter<?, ?> lineWrapperToLine = convertService.getHandler(ilwrap,
			Line.class);
		assertTrue(lineWrapperToLine instanceof WrapperToWritableLine);

		// Arrow to Line (shouldn't work)
		// Arrow is an ij.gui.Line but its contains method functions differently
		// from ij.gui.Line, and an Arrow and a Line which have the same end
		// coordinates have different measurements. So, Arrows cannot be converted
		// to imglib2 Line
		final Arrow a = new Arrow(10, 10, 100, 100);
		final Converter<?, ?> arrowToLine = convertService.getHandler(a,
			Line.class);
		assertTrue(arrowToLine == null);
	}

	@Test
	public void testIJLineToLineConverter() {
		final Line converted = convertService.convert(ijLine, Line.class);
		assertTrue(converted instanceof IJLineWrapper);

		assertEquals(ijwrap.endpointOne().getDoublePosition(0), //
			converted.endpointOne().getDoublePosition(0), 0);
		assertEquals(ijwrap.endpointOne().getDoublePosition(1), //
			converted.endpointOne().getDoublePosition(1), 0);
		assertEquals(ijwrap.endpointTwo().getDoublePosition(0), //
			converted.endpointTwo().getDoublePosition(0), 0);
		assertEquals(ijwrap.endpointTwo().getDoublePosition(1), //
			converted.endpointTwo().getDoublePosition(1), 0);
	}

	@Test
	public void testLineWrapperToLineConverter() {
		final Line result = convertService.convert(ilwrap, Line.class);
		assertTrue(result == ilLine);
	}

	// -- Line to ij.gui.Line converter tests --

	@Test
	public void testLineToIJLineConverterMatching() {
		// WritableLine to ij.gui.Line (should wrap)
		final Converter<?, ?> c = convertService.getHandler(ilLine,
			ij.gui.Line.class);
		assertTrue(c instanceof WritableLineToIJLineConverter);

		// Read only line to ij.gui.Line (shouldn't wrap)
		final TestLine test = new TestLine(0, 0, 10, 10);
		final Converter<?, ?> readOnlyToIjLine = convertService.getHandler(test,
			ij.gui.Line.class);
		assertTrue(readOnlyToIjLine instanceof LineToIJLineConverter);

		// Wrapped ij.gui.Line to ij.gui.Line (should unwrap)
		final Converter<?, ?> wrapc = convertService.getHandler(ijwrap,
			ij.gui.Line.class);
		assertTrue(wrapc instanceof WrapperToLineConverter);

		// WritableLine to Arrow (shouldn't work)
		final Converter<?, ?> arrow = convertService.getHandler(ilLine,
			Arrow.class);
		assertNull(arrow);

		// 3D Line to ij.gui.Line (shouldn't work)
		final Line ddd = new DefaultWritableLine(new double[] { 10.5, 7, -6.25 },
			new double[] { 106, -8.5, 21 }, false);
		final Converter<?, ?> multiDLine = convertService.getHandler(ddd,
			ij.gui.Line.class);
		assertNull(multiDLine);
	}

	@Test
	public void testLineToIJLineConverter() {
		final Line test = new TestLine(0, 1, 13, 44);
		final ij.gui.Line result = convertService.convert(test, ij.gui.Line.class);

		assertFalse(result instanceof LineWrapper);

		final RealLocalizable eOne = test.endpointOne();
		final RealLocalizable eTwo = test.endpointTwo();
		assertEquals(result.x1d, eOne.getDoublePosition(0), 0);
		assertEquals(result.y1d, eOne.getDoublePosition(1), 0);
		assertEquals(result.x2d, eTwo.getDoublePosition(0), 0);
		assertEquals(result.y2d, eTwo.getDoublePosition(1), 0);
	}

	@Test
	public void testWritableLineToIJLineConverter() {
		final ij.gui.Line result = convertService.convert(ilLine,
			ij.gui.Line.class);

		assertTrue(result instanceof LineWrapper);

		final RealLocalizable eOne = ilLine.endpointOne();
		final RealLocalizable eTwo = ilLine.endpointTwo();
		assertEquals(result.x1d, eOne.getDoublePosition(0), 0);
		assertEquals(result.y1d, eOne.getDoublePosition(1), 0);
		assertEquals(result.x2d, eTwo.getDoublePosition(0), 0);
		assertEquals(result.y2d, eTwo.getDoublePosition(1), 0);
	}

	@Test
	public void testLineToIJLineConverterWithLineWrapper() {
		final ij.gui.Line result = convertService.convert(ijwrap,
			ij.gui.Line.class);
		assertTrue(ijLine == result);
	}

	// -- Helper classes --

	private static final class TestLine implements Line {

		private final double x1;
		private final double y1;
		private final double x2;
		private final double y2;

		public TestLine(final double x1, final double y1, final double x2,
			final double y2)
		{
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}

		@Override
		public boolean test(final RealLocalizable t) {
			return false;
		}

		@Override
		public int numDimensions() {
			return 2;
		}

		@Override
		public double realMin(final int d) {
			if (d == 0) return x1 > x2 ? x2 : x1;
			return y1 > y2 ? y2 : y1;
		}

		@Override
		public double realMax(final int d) {
			if (d == 0) return x1 > x2 ? x1 : x2;
			return y1 > y2 ? y1 : y2;
		}

		@Override
		public RealLocalizable endpointOne() {
			return new RealPoint(new double[] { x1, y1 });
		}

		@Override
		public RealLocalizable endpointTwo() {
			return new RealPoint(new double[] { x2, y2 });
		}
	}

}
