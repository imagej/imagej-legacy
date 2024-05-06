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

package net.imagej.legacy.convert.roi.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ij.gui.Roi;

import net.imagej.legacy.convert.roi.MaskPredicateUnwrappers.WrapperToWritableBoxConverter;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToRoiConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.ClosedWritableBox;
import net.imglib2.roi.geom.real.WritableBox;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting between {@link Roi} and {@link Box}.
 *
 * @author Alison Walter
 */
public class RoiConversionTest {

	private Roi rect;
	private WritableBox wrapRect;
	private WritableBox b;
	private Roi wrapBox;
	private ConvertService convertService;

	@Before
	public void setup() {
		rect = new Roi(1, 13, 7, 4);
		wrapRect = new RoiWrapper(rect);
		b = new ClosedWritableBox(new double[] { 1, 13 }, new double[] { 8, 17 });
		wrapBox = new BoxWrapper(b);

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- To Box conversion tests --

	@Test
	public void testRoiToBoxConverterMatching() {
		// Roi to Box
		final Converter<?, ?> roiToBox = convertService.getHandler(rect, Box.class);
		assertTrue(roiToBox instanceof RoiToBoxConverter);

		// WrappedBox to Box
		final Converter<?, ?> wrapToBox = convertService.getHandler(wrapBox,
			Box.class);
		assertTrue(wrapToBox instanceof WrapperToWritableBoxConverter);

		// Roi w/ rounded corners to Box
		rect.setCornerDiameter(10);
		final Converter<?, ?> roundedCornerToBox = convertService.getHandler(rect,
			Box.class);
		assertTrue(roundedCornerToBox == null);
	}

	@Test
	public void testRoiToBoxConverter() {
		final Box converted = convertService.convert(rect, Box.class);

		assertTrue(converted instanceof RoiWrapper);

		assertEquals(wrapRect.center().getDoublePosition(0), converted.center()
			.getDoublePosition(0), 0);
		assertEquals(wrapRect.center().getDoublePosition(1), converted.center()
			.getDoublePosition(1), 0);
		assertEquals(converted.sideLength(0), converted.sideLength(0), 0);
		assertEquals(converted.sideLength(1), converted.sideLength(1), 0);
	}

	@Test
	public void testWrapperToBoxConversion() {
		final Box converted = convertService.convert(wrapBox, Box.class);

		assertTrue(converted == b);
	}

	// -- To Roi conversion tests --

	@Test
	public void testBoxToRoiConverterMatching() {
		final Converter<?, ?> cb = convertService.getHandler(b, Roi.class);
		assertTrue(cb instanceof WritableBoxToRoiConverter);

		final Converter<?, ?> cw = convertService.getHandler(wrapRect, Roi.class);
		assertTrue(cw instanceof WrapperToRoiConverter);

		final Converter<?, ?> cr = convertService.getHandler(new TestBox(),
			Roi.class);
		assertTrue(cr instanceof BoxToRoiConverter);

		final Box d = new ClosedWritableBox(new double[] { 0, -6.5, 12, 50 },
			new double[] { 2, 0.5, 6, 13.5 });
		final Converter<?, ?> cd = convertService.getHandler(d, Roi.class);
		assertNull(cd);
	}

	@Test
	public void testWritableBoxToRoiConversion() {
		final Roi r = convertService.convert(b, Roi.class);

		assertTrue(r instanceof BoxWrapper);
		assertEquals(((BoxWrapper) r).getSource(), b);

		assertEquals(Roi.RECTANGLE, r.getType());
		assertEquals(b.sideLength(0), r.getFloatWidth(), 0);
		assertEquals(b.sideLength(1), r.getFloatHeight(), 0);
		assertEquals(b.center().getDoublePosition(0) - b.sideLength(0) / 2, r
			.getXBase(), 0);
		assertEquals(b.center().getDoublePosition(1) - b.sideLength(1) / 2, r
			.getYBase(), 0);
	}

	@Test
	public void testBoxToRoiConversion() {
		final Box testBox = new TestBox();
		final Roi r = convertService.convert(testBox, Roi.class);

		assertFalse(r instanceof BoxWrapper);

		assertEquals(Roi.RECTANGLE, r.getType());
		assertEquals(testBox.sideLength(0), r.getFloatWidth(), 0);
		assertEquals(testBox.sideLength(1), r.getFloatHeight(), 0);
		assertEquals(testBox.center().getDoublePosition(0) - testBox.sideLength(0) /
			2, r.getXBase(), 0);
		assertEquals(testBox.center().getDoublePosition(1) - testBox.sideLength(1) /
			2, r.getYBase(), 0);
	}

	@Test
	public void testWrapperToRoiConversion() {
		final Roi r = convertService.convert(wrapRect, Roi.class);
		assertEquals(Roi.RECTANGLE, r.getType());
		assertTrue(rect == r);
	}

	// -- Helper classes --

	private static final class TestBox implements Box {

		private final double[] min;
		private final double[] max;

		public TestBox() {
			min = new double[] { 0, 0 };
			max = new double[] { 10, 10 };
		}

		@Override
		public boolean test(final RealLocalizable t) {
			return t.getDoublePosition(0) < max[0] && t.getDoublePosition(
				0) > min[0] && t.getDoublePosition(1) < max[1] && t.getDoublePosition(
					1) > min[1];
		}

		@Override
		public int numDimensions() {
			return 2;
		}

		@Override
		public double realMin(final int d) {
			return min[d];
		}

		@Override
		public double realMax(final int d) {
			return max[d];
		}

		@Override
		public double sideLength(final int d) {
			return max[d] - min[d];
		}

		@Override
		public RealLocalizable center() {
			return new RealPoint(new double[] { 5, 5 });
		}

	}
}
