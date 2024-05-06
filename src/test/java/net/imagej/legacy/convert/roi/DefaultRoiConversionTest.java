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

package net.imagej.legacy.convert.roi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Arrow;
import ij.gui.EllipseRoi;
import ij.gui.ImageRoi;
import ij.gui.Line;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.RotatedRectRoi;
import ij.gui.TextRoi;

import java.awt.Rectangle;

import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToEllipseRoiConverter;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToLineConverter;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToPolygonRoiConverter;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToRoiConverter;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToRotatedRectRoiConverter;
import net.imglib2.Point;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.MaskInterval;
import net.imglib2.roi.MaskPredicate;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting between ImageJ 1.x {@link Roi}s and ImgLib2
 * {@link MaskInterval}, and the corresponding wrapper
 * {@link DefaultRoiWrapper}.
 *
 * @author Alison Walter
 */
public class DefaultRoiConversionTest {

	private static Point test;
	private ConvertService convertService;

	@BeforeClass
	public static void initialize() {
		test = new Point(new int[] { 0, 0 });
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

	// -- Wrapper tests --

	@Test
	public void testDefaultRoiWrapperLineWithWidth() {
		final Line l = new Line(10, 10, 100, 100);
		ij.gui.Line.setWidth(5);
		l.setStrokeWidth(5);
		final MaskInterval w = new DefaultRoiWrapper<>(l);

		// Check bounds
		// Changing the line width does not cause the bounding box to update
		assertEquals(10, w.min(0));
		assertEquals(10, w.min(1));
		assertEquals(101, w.max(0));
		assertEquals(101, w.max(1));

		// Check contains
		test.setPosition(new int[] { 71, 71 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 12, 16 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 97, 94 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 9, 9 });
		assertFalse(w.test(test));
	}

	@Test
	public void testDefaultRoiWrapperSplineFitPolygonRoi() {
		final PolygonRoi p = new PolygonRoi(new float[] { 0, 15, 30 }, new float[] {
			10, 25, 10 }, Roi.POLYGON);
		p.fitSpline();
		final MaskInterval w = new DefaultRoiWrapper<>(p);

		// check bounds
		// spline fitting changes the bounds for a polygon
		final Rectangle bounds = p.getBounds();
		assertEquals(bounds.x, w.min(0));
		assertEquals(bounds.y, w.min(1));
		assertEquals(bounds.x + bounds.width, w.max(0));
		assertEquals(bounds.y + bounds.height, w.max(1));

		// Check contains
		test.setPosition(new int[] { 15, 6 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 5, 20 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 27, 17 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 30, 9 });
		assertFalse(w.test(test));
	}

	@Test
	public void testDefaultRoiWrapperRoundedCornerRectangle() {
		final Roi r = new Roi(17, -3, 10, 16, 10);
		final MaskInterval w = new DefaultRoiWrapper<>(r);

		// check bounds
		assertEquals(17, w.min(0));
		assertEquals(-3, w.min(1));
		assertEquals(27, w.max(0));
		assertEquals(13, w.max(1));

		// Check contains
		test.setPosition(new int[] { 20, 11 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 22, -1 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 17, 12 });
		assertFalse(w.test(test));

		test.setPosition(new int[] { 26, 12 });
		assertFalse(w.test(test));

		test.setPosition(new int[] { 17, -3 });
		assertFalse(w.test(test));

		test.setPosition(new int[] { 26, -3 });
		assertFalse(w.test(test));
	}

	@Test
	public void testDefaultRoiWrapperEllipseRoi() {
		final EllipseRoi e = new EllipseRoi(10, 11, 20, 21, 0.5);
		final MaskInterval w = new DefaultRoiWrapper<>(e);

		// check bounds
		// bounds are not the minimal bounds
		assertEquals(9, w.min(0));
		assertEquals(10, w.min(1));
		assertEquals(21, w.max(0));
		assertEquals(22, w.max(1));

		// Check contains
		test.setPosition(new int[] { 10, 11 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 18, 21 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 19, 14 });
		assertFalse(w.test(test));
	}

	@Test
	public void testDefaultRoiWrapperRotatedRectRoi() {
		final RotatedRectRoi rrr = new RotatedRectRoi(-3.5, 27, 30.5, 61, 6);
		final MaskInterval w = new DefaultRoiWrapper<>(rrr);

		// check bounds
		assertEquals(-6, w.min(0));
		assertEquals(24, w.min(1));
		assertEquals(33, w.max(0));
		assertEquals(63, w.max(1));

		// Check contains
		test.setPosition(new int[] { 31, 58 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { -1, 32 });
		assertTrue(w.test(test));

		test.setPosition(new int[] { 1, 26 });
		assertFalse(w.test(test));
	}

	// -- To MaskInterval conversion tests --

	@Test
	public void testRoiToMaskIntervalConverterLineWithWidth() {
		final Line l = new Line(10, 10, 100, 100);
		ij.gui.Line.setWidth(5);
		l.setStrokeWidth(5);
		final MaskInterval converted = convertService.convert(l,
			MaskInterval.class);

		assertTrue(converted instanceof DefaultRoiWrapper);
	}

	@Test
	public void testRoiToMaskIntervalConverterSplineFitPolygonRoi() {
		final PolygonRoi p = new PolygonRoi(new float[] { 0, 15, 30 }, new float[] {
			10, 25, 10 }, Roi.POLYGON);
		p.fitSpline();
		final MaskInterval converted = convertService.convert(p,
			MaskInterval.class);

		assertTrue(converted instanceof DefaultRoiWrapper);
	}

	@Test
	public void testRoiToMaskIntervalConverterRoundedCornerRectangle() {
		final Roi r = new Roi(17, -3, 10, 16, 10);
		final MaskInterval converted = convertService.convert(r,
			MaskInterval.class);

		assertTrue(converted instanceof DefaultRoiWrapper);
	}

	@Test
	public void testRoiToMaskIntervalConverterEllipseRoi() {
		final EllipseRoi e = new EllipseRoi(10, 11, 20, 21, 0.5);
		final MaskInterval converted = convertService.convert(e,
			MaskInterval.class);

		assertTrue(converted instanceof DefaultRoiWrapper);
	}

	@Test
	public void testRoiToMaskIntervalConverterRotatedRectRoi() {
		final RotatedRectRoi rrr = new RotatedRectRoi(-3.5, 27, 30.5, 61, 6);
		final MaskInterval converted = convertService.convert(rrr,
			MaskInterval.class);

		assertTrue(converted instanceof DefaultRoiWrapper);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRoiToMaskIntervalConverterTextRoi() {
		final TextRoi t = new TextRoi(10, 10, "text");
		final MaskPredicate<? extends RealLocalizable> converted = convertService
			.convert(t, MaskPredicate.class);

		assertTrue(converted == null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRoiToMaskIntervalConverterArrow() {
		final Arrow a = new Arrow(12, 13, 90, 76);
		final MaskPredicate<? extends RealLocalizable> converted = convertService
			.convert(a, MaskPredicate.class);

		assertTrue(converted == null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testRoiToMaskIntervalConverterImageRoi() {
		final ImagePlus img = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
		final ImageRoi i = new ImageRoi(12, 8, img.getBufferedImage());
		final MaskPredicate<? extends RealLocalizable> converted = convertService
			.convert(i, MaskPredicate.class);

		assertTrue(converted == null);
	}

	// -- Unwrapping tests --

	@Test
	public void testWrapLineWithWidthConversion() {
		final Line l = new Line(10, 10, 100, 100);
		ij.gui.Line.setWidth(5);
		l.setStrokeWidth(5);
		final MaskInterval m = new DefaultRoiWrapper<>(l);
		final Converter<?, ?> c = convertService.getHandler(m, Line.class);

		assertTrue(c instanceof WrapperToLineConverter);

		final Line cl = convertService.convert(m, Line.class);
		assertEquals(Roi.LINE, cl.getType());
		assertTrue(l == cl);
	}

	@Test
	public void testWrapSplineFitPolygonConversion() {
		final PolygonRoi p = new PolygonRoi(new float[] { 0, 15, 30 }, new float[] {
			10, 25, 10 }, Roi.POLYGON);
		p.fitSpline();
		final MaskInterval m = new DefaultRoiWrapper<>(p);
		final Converter<?, ?> c = convertService.getHandler(m, PolygonRoi.class);

		assertTrue(c instanceof WrapperToPolygonRoiConverter);

		final PolygonRoi cp = convertService.convert(m, PolygonRoi.class);
		assertEquals(Roi.POLYGON, cp.getType());
		assertTrue(p == cp);
	}

	@Test
	public void testWrapRoundedCornerRectangleConversion() {
		final Roi r = new Roi(17, -3, 10, 16, 10);
		final MaskInterval m = new DefaultRoiWrapper<>(r);
		final Converter<?, ?> c = convertService.getHandler(m, Roi.class);

		assertTrue(c instanceof WrapperToRoiConverter);

		final Roi cr = convertService.convert(m, Roi.class);
		assertEquals(Roi.RECTANGLE, cr.getType());
		assertTrue(r == cr);
	}

	@Test
	public void testWrapEllipseRoiConversion() {
		final EllipseRoi e = new EllipseRoi(10, 11, 20, 21, 0.5);
		final MaskInterval m = new DefaultRoiWrapper<>(e);
		final Converter<?, ?> c = convertService.getHandler(m, EllipseRoi.class);

		assertTrue(c instanceof WrapperToEllipseRoiConverter);

		final EllipseRoi ce = convertService.convert(m, EllipseRoi.class);
		assertEquals(Roi.FREEROI, ce.getType());
		assertTrue(e == ce);
	}

	@Test
	public void testWrapRotatedRectRoiConversion() {
		final RotatedRectRoi rrr = new RotatedRectRoi(-3.5, 27, 30.5, 61, 6);
		final MaskInterval m = new DefaultRoiWrapper<>(rrr);
		final Converter<?, ?> c = convertService.getHandler(m,
			RotatedRectRoi.class);

		assertTrue(c instanceof WrapperToRotatedRectRoiConverter);

		final RotatedRectRoi crrr = convertService.convert(m, RotatedRectRoi.class);
		assertEquals(Roi.FREEROI, crrr.getType());
		assertTrue(rrr == crrr);
	}
}
