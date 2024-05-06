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

package net.imagej.legacy.convert.roi.ellipsoid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ij.gui.OvalRoi;

import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToOvalRoiConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.ClosedWritableEllipsoid;
import net.imglib2.roi.geom.real.ClosedWritableSphere;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.OpenWritableEllipsoid;
import net.imglib2.roi.geom.real.Sphere;
import net.imglib2.roi.geom.real.WritableEllipsoid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting between {@link OvalRoi} and {@link Ellipsoid}.
 *
 * @author Alison Walter
 */
public class OvalRoiConversionTest {

	private OvalRoi oval;
	private WritableEllipsoid e;
	private WritableEllipsoid wrapOval;
	private OvalRoi wrapEllipsoid;
	private ConvertService convertService;

	@Before
	public void setup() {
		oval = new OvalRoi(10, 22, 7, 4);
		e = new ClosedWritableEllipsoid(new double[] { 13.5, 24 }, new double[] {
			3.5, 2 });
		wrapOval = new OvalRoiWrapper(oval);
		wrapEllipsoid = new EllipsoidWrapper(e);

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- OvalRoiToEllipsoidConverter tests --

	@Test
	public void testOvalRoiToEllipsoidConverterMatching() {
		// OvalRoi to Ellipsoid (should wrap)

		// EllipsoidWrapper to Ellipsoid (should unwrap)
	}

	@Test
	public void testOvalRoiToEllipsoidConverter() {
		final Ellipsoid converted = convertService.convert(oval, Ellipsoid.class);

		assertTrue(converted instanceof OvalRoiWrapper);
		assertEquals(oval.getXBase() + oval.getFloatWidth() / 2, converted.center()
			.getDoublePosition(0), 0);
		assertEquals(oval.getYBase() + oval.getFloatHeight() / 2, converted.center()
			.getDoublePosition(1), 0);
		assertEquals(oval.getFloatWidth() / 2, converted.semiAxisLength(0), 0);
		assertEquals(oval.getFloatHeight() / 2, converted.semiAxisLength(1), 0);
	}

	@Test
	public void testEllipsoidWrapperToEllipsoidConverter() {
		final Ellipsoid converted = convertService.convert(wrapEllipsoid,
			Ellipsoid.class);

		assertTrue(converted == e);
	}

	// -- EllipsoidToOvalRoiConverter tests --

	@Test
	public void testEllipsoidToOvalRoiConverterMatching() {
		// Writable Ellipsoid to OvalRoi (should wrap)
		final Converter<?, ?> c = convertService.getHandler(e, OvalRoi.class);
		assertTrue(c instanceof WritableEllipsoidToOvalRoiConverter);

		// Wrapped OvalRoi to OvalRoi (should unwrap)
		final Converter<?, ?> cc = convertService.getHandler(wrapOval,
			OvalRoi.class);
		assertTrue(cc instanceof WrapperToOvalRoiConverter);

		// Writable Sphere to OvalRoi (should wrap)
		final Sphere s = new ClosedWritableSphere(new double[] { 1.25, -13.5 }, 10);
		final Converter<?, ?> ccc = convertService.getHandler(s, OvalRoi.class);
		assertTrue(ccc instanceof WritableEllipsoidToOvalRoiConverter);

		// Read Only Ellipsoid to OvalRoi (should not wrap)
		final Converter<?, ?> ccccc = convertService.getHandler(new TestEllipsoid(),
			OvalRoi.class);
		assertTrue(ccccc instanceof EllipsoidToOvalRoiConverter);

		// 3D Ellipsoid to OvalRoi (not possible)
		final Ellipsoid oe = new OpenWritableEllipsoid(new double[] { 1.5, 6.25, -9,
			62.125 }, new double[] { 11, 1, 0.5, 107 });
		final Converter<?, ?> cccc = convertService.getHandler(oe, OvalRoi.class);
		assertNull(cccc);
	}

	@Test
	public void testEllipsoidToOvalRoiConverterWithWritableEllipsoid() {
		final OvalRoi o = convertService.convert(e, OvalRoi.class);

		assertTrue(o instanceof EllipsoidWrapper);
		assertTrue(((EllipsoidWrapper) o).getSource() == e);

		final RealLocalizable center = e.center();
		assertEquals(center.getDoublePosition(0), o.getXBase() + o.getFloatWidth() /
			2, 0);
		assertEquals(center.getDoublePosition(1), o.getYBase() + o
			.getFloatHeight() / 2, 0);
		assertEquals(e.semiAxisLength(0), o.getFloatWidth() / 2, 0);
		assertEquals(e.semiAxisLength(1), o.getFloatHeight() / 2, 0);
	}

	@Test
	public void testEllipsoidToOvalRoiConverterWithEllipsoid() {
		final Ellipsoid test = new TestEllipsoid();
		final OvalRoi o = convertService.convert(test, OvalRoi.class);

		assertFalse(o instanceof EllipsoidWrapper);
		final RealLocalizable center = test.center();
		assertEquals(center.getDoublePosition(0), o.getXBase() + o.getFloatWidth() /
			2, 0);
		assertEquals(center.getDoublePosition(1), o.getYBase() + o
			.getFloatHeight() / 2, 0);
		assertEquals(test.semiAxisLength(0), o.getFloatWidth() / 2, 0);
		assertEquals(test.semiAxisLength(1), o.getFloatHeight() / 2, 0);
	}

	@Test
	public void testEllipsoidToOvalRoiConverterWithWrapper() {
		final OvalRoi o = convertService.convert(wrapOval, OvalRoi.class);
		assertTrue(oval == o);
	}

	// -- Helper classes --

	private static final class TestEllipsoid implements Ellipsoid {

		private final RealPoint center;
		private final double[] semiAxis;

		public TestEllipsoid() {
			center = new RealPoint(10, 12);
			semiAxis = new double[] { 3, 4 };
		}

		@Override
		public double exponent() {
			return 2;
		}

		@Override
		public double semiAxisLength(final int d) {
			return semiAxis[d];
		}

		@Override
		public RealLocalizable center() {
			return center;
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
			return center.getDoublePosition(d) - semiAxis[d];
		}

		@Override
		public double realMax(final int d) {
			return center.getDoublePosition(d) + semiAxis[d];
		}

	}
}
