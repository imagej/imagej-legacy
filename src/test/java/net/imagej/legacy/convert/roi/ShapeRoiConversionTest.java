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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToShapeRoiConverter;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.roi.RealMask;
import net.imglib2.roi.RealMaskRealInterval;
import net.imglib2.roi.composite.CompositeMaskPredicate;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.ClosedWritableBox;
import net.imglib2.roi.geom.real.ClosedWritableEllipsoid;
import net.imglib2.roi.geom.real.ClosedWritableSphere;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.OpenWritableBox;
import net.imglib2.roi.geom.real.OpenWritableEllipsoid;
import net.imglib2.roi.geom.real.OpenWritableSphere;
import net.imglib2.roi.geom.real.Sphere;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting an ImageJ 1.x {@link ShapeRoi} to an ImgLib2
 * {@link RealMaskRealInterval} and the corresponding {@link ShapeRoiWrapper}.
 * This also tests converting a {@link CompositeMaskPredicate} to a
 * {@code ShapeRoi}.
 *
 * @author Alison Walter
 */
public class ShapeRoiConversionTest {

	private ShapeRoi shape;
	private RealMaskRealInterval wrap;
	private ConvertService convertService;

	private Box cb;
	private Box ob;
	private Sphere cs;
	private Sphere ops;
	private Ellipsoid ce;
	private Ellipsoid oe;

	@Before
	public void setup() {
		final Roi rect = new Roi(100, 250, 12.5, 31);
		final OvalRoi oval = new OvalRoi(107, 240, 35.5, 17);
		final ShapeRoi rs = new ShapeRoi(rect);
		final ShapeRoi os = new ShapeRoi(oval);

		shape = rs.or(os);
		wrap = new ShapeRoiWrapper(shape);

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);

		cb = new ClosedWritableBox(new double[] { 10, 12 }, new double[] { 123,
			80 });
		ob = new OpenWritableBox(new double[] { -9, 63 }, new double[] { 12, 92 });
		cs = new ClosedWritableSphere(new double[] { 53, 61 }, 5);
		ops = new OpenWritableSphere(new double[] { 50, 51 }, 10);
		oe = new OpenWritableEllipsoid(new double[] { 0, 0 }, new double[] { 3,
			5 });
		ce = new ClosedWritableEllipsoid(new double[] { 15, 70 }, new double[] {
			8.5, 1 });
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- Wrapper tests --

	@Test
	public void testShapeRoiWrapperTest() {
		final RealPoint test = new RealPoint(new double[] { 105, 280 });
		assertEquals(wrap.test(test), shape.contains(105, 280)); // in rectangle
		test.setPosition(new double[] { 130, 241 });
		assertEquals(wrap.test(test), shape.contains(130, 241)); // in oval
		test.setPosition(new double[] { 111, 253 });
		assertEquals(wrap.test(test), shape.contains(111, 253)); // in both
		test.setPosition(new double[] { 12, 16 });
		assertEquals(wrap.test(test), shape.contains(12, 16)); // in neither

		test.setPosition(new double[] { 100.25, 251.5 });
		assertTrue(wrap.test(test));
		test.setPosition(new double[] { 112, 281.125 });
		assertFalse(wrap.test(test));
	}

	@Test
	public void testShapeRoiWrapperBounds() {
		assertEquals(100, wrap.realMin(0), 0);
		assertEquals(240, wrap.realMin(1), 0);
		// computing the new bounds in ShapeRoi uses the integer space bounds
		// instead of the real bounds. So this is 143 instead of 142.5
		assertEquals(143, wrap.realMax(0), 0);
		assertEquals(281, wrap.realMax(1), 0);
	}

	// -- from ShapeRoi conversion tests --

	@Test
	public void testShapeRoiToMaskRealIntervalConverter() {
		final RealMaskRealInterval converted = convertService.convert(shape,
			RealMaskRealInterval.class);

		assertTrue(converted instanceof ShapeRoiWrapper);

		assertEquals(wrap.realMin(0), converted.realMin(0), 0);
		assertEquals(wrap.realMin(1), converted.realMin(1), 0);
		assertEquals(wrap.realMax(0), converted.realMax(0), 0);
		assertEquals(wrap.realMax(1), converted.realMax(1), 0);
	}

	// -- to ShapeRoi conversion tests --

	@Test
	public void testMaskOperationResultToShapeRoiConverterMatching() {
		// Unwrap
		final Converter<?, ?> unwrap = convertService.getHandler(wrap,
			ShapeRoi.class);
		assertTrue(unwrap instanceof WrapperToShapeRoiConverter);

		// AND
		final RealMaskRealInterval and = cb.and(cs);
		final Converter<?, ?> andC = convertService.getHandler(and, ShapeRoi.class);
		assertTrue(andC instanceof BinaryCompositeMaskPredicateToShapeRoiConverter);

		// N-ary OR then And
		final RealMaskRealInterval nary = ops.and(cb.or(cs).or(ce));
		final Converter<?, ?> naryC = convertService.getHandler(nary,
			ShapeRoi.class);
		assertTrue(
			naryC instanceof BinaryCompositeMaskPredicateToShapeRoiConverter);

		// Not
		final RealMask not = ops.negate();
		final Converter<?, ?> notC = convertService.getHandler(not, ShapeRoi.class);
		assertNull(notC);

		// Rotate then OR, then AND
		final AffineTransform2D t = new AffineTransform2D();
		t.rotate(Math.PI / 2);
		final RealMask rotate = oe.and(ops.or(ob.transform(t.inverse())));
		final Converter<?, ?> rotateC = convertService.getHandler(rotate,
			ShapeRoi.class);
		assertNull(rotateC);
	}

	@Test
	public void testWrappedShapeRoiToShapeRoi() {
		final ShapeRoi s = convertService.convert(wrap, ShapeRoi.class);
		assertTrue(s == shape);
	}

	@Test
	public void testMaskOperationResultToShapeRoiConverterAnd() {
		final RealMaskRealInterval and = cb.and(cs);
		final ShapeRoi s = convertService.convert(and, ShapeRoi.class);

		assertEquals(and.realMin(0), s.getXBase(), 0);
		assertEquals(and.realMin(1), s.getYBase(), 0);
		assertEquals(and.realMax(0), s.getXBase() + s.getFloatWidth(), 0);
		assertEquals(and.realMax(0), s.getXBase() + s.getFloatHeight(), 0);

		final RealPoint test = new RealPoint(new double[] { 53, 61 });
		assertEquals(and.test(test), s.contains(53, 61));
		test.setPosition(new double[] { 49, 65 });
		assertEquals(and.test(test), s.contains(49, 65));
	}

	@Test
	public void testMaskOperationResultToShapeRoiConverterSubtract() {
		final RealMaskRealInterval sub = cb.and(cs).minus(ops);
		final ShapeRoi s = convertService.convert(sub, ShapeRoi.class);

		// Can't just use subtraction bounds, because subtraction just maintains
		// the bounds of its first input
		assertEquals(sub.realMin(0), s.getXBase(), 0);
		assertEquals(59, s.getYBase(), 0);
		assertEquals(sub.realMax(0), s.getXBase() + s.getFloatWidth(), 0);
		assertEquals(sub.realMax(1), s.getYBase() + s.getFloatHeight(), 0);

		final RealPoint test = new RealPoint(new double[] { 56, 64 });
		assertEquals(sub.test(test), s.contains(56, 64));
		test.setPosition(new double[] { 58, 62 });
		assertEquals(sub.test(test), s.contains(58, 62));
	}

	@Test
	public void testMaskOperationResultToShapeRoiConverterMultiple() {
		final RealMaskRealInterval multi = cb.and(cs).or(oe.or(ce).minus(ob).or(ops
			.xor(cb)));
		final ShapeRoi s = convertService.convert(multi, ShapeRoi.class);

		assertEquals(multi.realMin(0), s.getXBase(), 0);
		assertEquals(multi.realMin(1), s.getYBase(), 0);
		assertEquals(multi.realMax(0), s.getXBase() + s.getFloatWidth(), 0);
		assertEquals(multi.realMax(1), s.getYBase() + s.getFloatHeight(), 0);
	}
}
