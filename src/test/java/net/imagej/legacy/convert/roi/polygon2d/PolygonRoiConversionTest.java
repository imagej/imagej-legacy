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

package net.imagej.legacy.convert.roi.polygon2d;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

import net.imagej.legacy.convert.roi.MaskPredicateUnwrappers.WrapperToWritablePolygon2D;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToPolygonRoiConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.WritablePolygon2D;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting {@link PolygonRoi} to {@link Polygon2D}.
 *
 * @author Alison Walter
 */
public class PolygonRoiConversionTest {

	private PolygonRoi poly;
	private WritablePolygon2D wrap;
	private PolygonRoi free;
	private Polygon2D freeWrap;
	private PolygonRoi traced;
	private Polygon2D tracedWrap;
	private WritablePolygon2D p2d;
	private PolygonRoi p2dwrap;
	private ConvertService convertService;

	@Before
	public void setup() {
		poly = new PolygonRoi(new float[] { 100.5f, 100.5f, 150, 199, 199 },
			new float[] { 100, 200, 250.25f, 200, 100 }, Roi.POLYGON);
		wrap = new PolygonRoiWrapper(poly);

		final int[] x = new int[] { 21, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6,
			6, 5, 5, 4, 4, 3, 3, 3, 2, 2, 2, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 2,
			3, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 19, 19,
			20, 21, 22, 22, 23, 23, 24, 24, 24, 25, 25, 26, 26, 27, 27, 27, 27 };
		final int[] y = new int[] { 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2,
			3, 4, 4, 5, 6, 6, 7, 8, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
			21, 21, 22, 22, 22, 22, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
			23, 22, 21, 20, 18, 17, 16, 15, 14, 13, 12, 11, 9, 8, 8, 7, 7, 6, 5, 4 };
		free = new PolygonRoi(x, y, x.length, Roi.FREEROI);
		freeWrap = new UnmodifiablePolygonRoiWrapper(free);

		final int[] xt = new int[] { 8, 7, 7, 0, 0, 1, 1, 8 };
		final int[] yt = new int[] { 4, 4, 5, 5, 2, 2, 0, 0 };
		traced = new PolygonRoi(xt, yt, xt.length, Roi.TRACED_ROI);
		tracedWrap = new UnmodifiablePolygonRoiWrapper(traced);

		p2d = new DefaultWritablePolygon2D(new double[] { 1, 17, 30 },
			new double[] { 33, 40, 40 });
		p2dwrap = new Polygon2DWrapper(p2d);

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- to Polygon2D conversion tests --

	@Test
	public void testPolygonRoiToPolygon2DConverterMatching() {
		// PolygonRoi to Polygon2D (should wrap)
		final Converter<?, ?> prToP2d = convertService.getHandler(poly,
			Polygon2D.class);
		assertTrue(prToP2d instanceof PolygonRoiToPolygon2DConverter);

		// FreeRoi to Polygon2D (should wrap)
		final Converter<?, ?> frToP2d = convertService.getHandler(free,
			Polygon2D.class);
		assertTrue(frToP2d instanceof PolygonRoiToPolygon2DConverter);

		// TracedRoi to Polygon2D (should wrap)
		final Converter<?, ?> trToP2d = convertService.getHandler(traced,
			Polygon2D.class);
		assertTrue(trToP2d instanceof PolygonRoiToPolygon2DConverter);

		// Spline fit to Polygon2D (shouldn't work)
		poly.fitSpline();
		final Converter<?, ?> splinePrToP2d = convertService.getHandler(poly,
			Polygon2D.class);
		assertTrue(splinePrToP2d == null);

		// Wrapped Polygon2D to Polygon2D (should unwrap)
		final Converter<?, ?> wrapperToP2d = convertService.getHandler(p2dwrap,
			Polygon2D.class);
		assertTrue(wrapperToP2d instanceof WrapperToWritablePolygon2D);
	}

	@Test
	public void testPolygonRoiToPolygon2D() {
		final Polygon2D converted = convertService.convert(poly, Polygon2D.class);

		assertTrue(converted instanceof PolygonRoiWrapper);

		assertEquals(5, converted.numVertices());
		assertEquals(wrap.vertex(0).getDoublePosition(0), converted.vertex(0)
			.getDoublePosition(0), 0);
		assertEquals(wrap.vertex(0).getDoublePosition(1), converted.vertex(0)
			.getDoublePosition(1), 0);
		assertEquals(wrap.vertex(1).getDoublePosition(0), converted.vertex(1)
			.getDoublePosition(0), 0);
		assertEquals(wrap.vertex(1).getDoublePosition(1), converted.vertex(1)
			.getDoublePosition(1), 0);
		assertEquals(wrap.vertex(2).getDoublePosition(0), converted.vertex(2)
			.getDoublePosition(0), 0);
		assertEquals(wrap.vertex(2).getDoublePosition(1), converted.vertex(2)
			.getDoublePosition(1), 0);
		assertEquals(wrap.vertex(3).getDoublePosition(0), converted.vertex(3)
			.getDoublePosition(0), 0);
		assertEquals(wrap.vertex(3).getDoublePosition(1), converted.vertex(3)
			.getDoublePosition(1), 0);
		assertEquals(wrap.vertex(4).getDoublePosition(0), converted.vertex(4)
			.getDoublePosition(0), 0);
		assertEquals(wrap.vertex(4).getDoublePosition(1), converted.vertex(4)
			.getDoublePosition(1), 0);
	}

	@Test
	public void testFreeRoiToPolygon2D() {
		final Polygon2D converted = convertService.convert(free, Polygon2D.class);

		assertTrue(converted instanceof UnmodifiablePolygonRoiWrapper);

		final float[] xp = free.getFloatPolygon().xpoints;
		final float[] yp = free.getFloatPolygon().ypoints;

		assertEquals(free.getNCoordinates(), converted.numVertices());
		for (int i = 0; i < free.getNCoordinates(); i++) {
			assertEquals(xp[i], converted.vertex(i).getDoublePosition(0), 0);
			assertEquals(yp[i], converted.vertex(i).getDoublePosition(1), 0);
		}
	}

	@Test
	public void testTracedRoiToPolygon2D() {
		final Polygon2D converted = convertService.convert(traced, Polygon2D.class);

		assertTrue(converted instanceof UnmodifiablePolygonRoiWrapper);

		final float[] xp = traced.getFloatPolygon().xpoints;
		final float[] yp = traced.getFloatPolygon().ypoints;

		assertEquals(traced.getNCoordinates(), converted.numVertices());
		for (int i = 0; i < traced.getNCoordinates(); i++) {
			assertEquals(xp[i], converted.vertex(i).getDoublePosition(0), 0);
			assertEquals(yp[i], converted.vertex(i).getDoublePosition(1), 0);
		}
	}

	@Test
	public void testWrapperToPolygon2D() {
		final Polygon2D converted = convertService.convert(p2dwrap,
			Polygon2D.class);
		assertTrue(p2d == converted);
	}

	// -- to PolygonRoi conversion tests --

	@Test
	public void testPolygonRoiConverterMatching() {
		// WritablePolygon2D to PolygonRoi (should wrap)
		final Converter<?, ?> c = convertService.getHandler(p2d, PolygonRoi.class);
		assertTrue(c instanceof WritablePolygon2DToPolygonRoiConverter);

		// Read only Polygon2D to Polygon2D (shouldn't wrap)
		final Polygon2D readOnly = new TestPolygon2D();
		final Converter<?, ?> readOnlyP2dToPr = convertService.getHandler(readOnly,
			PolygonRoi.class);
		assertTrue(readOnlyP2dToPr instanceof Polygon2DToPolygonRoiConverter);

		// WrappedPolyonRoi to PolygonRoi (should unwrap)
		final Converter<?, ?> pWrap = convertService.getHandler(wrap,
			PolygonRoi.class);
		assertTrue(pWrap instanceof WrapperToPolygonRoiConverter);

		// WrappedTracedRoi to PolygonRoi (should unwrap)
		final Converter<?, ?> tWrap = convertService.getHandler(tracedWrap,
			PolygonRoi.class);
		assertTrue(tWrap instanceof WrapperToPolygonRoiConverter);

		// WrappedFreeRoi to PolygonRoi (should unwrap)
		final Converter<?, ?> fWrap = convertService.getHandler(freeWrap,
			PolygonRoi.class);
		assertTrue(fWrap instanceof WrapperToPolygonRoiConverter);
	}

	@Test
	public void testPolygon2DToPolygonRoi() {
		final Polygon2D test = new TestPolygon2D();
		final PolygonRoi pr = convertService.convert(test, PolygonRoi.class);

		assertFalse(pr instanceof Polygon2DWrapper);
		assertEquals(Roi.POLYGON, pr.getType());
		assertEquals(test.numVertices(), pr.getNCoordinates());
		final float[] x = pr.getFloatPolygon().xpoints;
		final float[] y = pr.getFloatPolygon().ypoints;
		for (int i = 0; i < test.numVertices(); i++) {
			final RealLocalizable v = test.vertex(i);
			assertEquals(v.getDoublePosition(0), x[i], 0);
			assertEquals(v.getDoublePosition(1), y[i], 0);
		}
	}

	@Test
	public void testWritablePolygon2DToPolygonRoi() {
		final PolygonRoi pr = convertService.convert(p2d, PolygonRoi.class);

		assertTrue(pr instanceof Polygon2DWrapper);
		assertEquals(Roi.POLYGON, pr.getType());
		assertEquals(p2d.numVertices(), pr.getNCoordinates());
		final float[] x = pr.getFloatPolygon().xpoints;
		final float[] y = pr.getFloatPolygon().ypoints;
		for (int i = 0; i < p2d.numVertices(); i++) {
			final RealLocalizable v = p2d.vertex(i);
			assertEquals(v.getDoublePosition(0), x[i], 0);
			assertEquals(v.getDoublePosition(1), y[i], 0);
		}
	}

	@Test
	public void testWrappedPolygonToPolygonRoi() {
		final PolygonRoi pr = convertService.convert(wrap, PolygonRoi.class);
		assertEquals(Roi.POLYGON, pr.getType());
		assertTrue(poly == pr);
	}

	@Test
	public void testWrappedFreelineToPolygonRoi() {
		final PolygonRoi pr = convertService.convert(freeWrap, PolygonRoi.class);
		assertEquals(Roi.FREEROI, pr.getType());
		assertTrue(free == pr);
	}

	@Test
	public void testWrappedTracedToPolygonRoi() {
		final PolygonRoi pr = convertService.convert(tracedWrap, PolygonRoi.class);
		assertEquals(Roi.TRACED_ROI, pr.getType());
		assertTrue(traced == pr);
	}

	// -- Helper classes --

	private static final class TestPolygon2D implements Polygon2D {

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
			return 0;
		}

		@Override
		public double realMax(final int d) {
			return d == 0 ? 10 : 5;
		}

		@Override
		public RealLocalizable vertex(final int pos) {
			if (pos == 0) return new RealPoint(new double[] { 0, 0 });
			else if (pos == 1) return new RealPoint(new double[] { 5, 5 });
			return new RealPoint(new double[] { 10, 0 });
		}

		@Override
		public int numVertices() {
			return 3;
		}

	}
}
