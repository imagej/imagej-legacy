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

package net.imagej.legacy.convert.roi.polyline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

import java.util.ArrayList;
import java.util.List;

import net.imagej.legacy.convert.roi.MaskPredicateUnwrappers.WrapperToWritablePolyline;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToPolygonRoiConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritablePolyline;
import net.imglib2.roi.geom.real.Polyline;
import net.imglib2.roi.geom.real.WritablePolyline;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting between {@link PolygonRoi} and {@link Polyline}.
 *
 * @author Alison Walter
 */
public class PolylineRoiConversionTest {

	private PolygonRoi poly;
	private WritablePolyline wrap;
	private PolygonRoi free;
	private Polyline freeWrap;
	private PolygonRoi angle;
	private Polyline angleWrap;
	private WritablePolyline polyline;
	private PolygonRoi wrapPolyline;
	private ConvertService convertService;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		poly = new PolygonRoi(new float[] { 1.25f, 20, 50, 79 }, new float[] {
			1.25f, 20, -30, -1 }, Roi.POLYLINE);
		poly.setStrokeWidth(0);
		wrap = new PolylineRoiWrapper(poly);

		final int[] xf = new int[] { 143, 136, 128, 126, 124, 123, 122, 121, 120,
			118, 118, 117, 116, 116, 116, 115, 115, 115, 115, 115, 115, 115, 115, 116,
			116, 117, 117, 117, 118, 119, 119, 119, 120, 120, 121, 122, 122, 123, 124,
			125, 126, 127, 128, 129, 130, 131, 131, 132, 133, 132, 133, 135, 136, 137,
			138, 140, 141, 142, 143, 144, 144, 146, 147, 147, 148, 149, 150, 152, 153,
			153, 154, 156, 157, 158, 158, 158, 158, 158, 158, 158, 157, 157, 156, 155,
			155, 154, 154, 153, 153, 152, 151, 149, 149, 148, 147, 146, 146, 145, 144,
			143, 142, 141, 140, 140, 140, 140, 141, 141, 142, 143, 144, 144, 145, 146,
			147, 149, 150, 151, 152, 152, 153, 154, 155, 156, 157, 158, 159, 161, 162,
			166, 168, 169, 170, 171, 172, 172, 174, 175, 175, 176, 177, 177, 179, 180,
			180, 181, 182, 183, 183 };
		final int[] yf = new int[] { 37, 37, 34, 34, 34, 34, 34, 34, 35, 39, 40, 42,
			42, 44, 45, 45, 46, 47, 48, 49, 50, 51, 52, 54, 55, 57, 58, 59, 60, 61,
			62, 63, 63, 64, 65, 65, 66, 67, 67, 68, 69, 69, 70, 71, 71, 71, 72, 72,
			72, 72, 72, 72, 72, 72, 72, 73, 73, 73, 73, 73, 74, 74, 74, 75, 75, 76,
			76, 76, 76, 77, 78, 79, 80, 81, 82, 83, 85, 86, 87, 88, 89, 90, 91, 92,
			94, 95, 96, 96, 97, 98, 98, 99, 100, 100, 100, 101, 102, 102, 102, 102,
			103, 103, 103, 104, 105, 106, 106, 107, 107, 108, 108, 109, 109, 109, 109,
			110, 110, 110, 110, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 113,
			113, 114, 114, 114, 115, 116, 118, 119, 120, 120, 121, 122, 123, 124, 125,
			125, 126, 128, 129 };
		free = new PolygonRoi(xf, yf, xf.length, Roi.FREELINE);
		free.setStrokeWidth(0);
		freeWrap = new UnmodifiablePolylineRoiWrapper(free);

		angle = new PolygonRoi(new int[] { 166, 80, 163 }, new int[] { 79, 122,
			126 }, 3, Roi.ANGLE);
		angle.setStrokeWidth(0);
		angleWrap = new UnmodifiablePolylineRoiWrapper(angle);

		final List<RealLocalizable> pts = new ArrayList<>(3);
		pts.add(new RealPoint(new double[] { 1, 1 }));
		pts.add(new RealPoint(new double[] { 10, 10 }));
		pts.add(new RealPoint(new double[] { 20, 5 }));
		polyline = new DefaultWritablePolyline(pts);
		wrapPolyline = new PolylineWrapper(polyline);

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- To Polyline conversion tests --

	@Test
	public void testPolylineRoiToPolylineConverterMatching() {
		// PolylineRoi to Polyline (should wrap)
		final Converter<?, ?> prToPolyline = convertService.getHandler(poly,
			Polyline.class);
		assertTrue(prToPolyline instanceof PolylineRoiToPolylineConverter);

		// WrappedPolyline to Polyline (should unwrap)
		final Converter<?, ?> wrappedToPolyline = convertService.getHandler(
			wrapPolyline, Polyline.class);
		assertTrue(wrappedToPolyline instanceof WrapperToWritablePolyline);

		// Freeline to Polyline (should wrap)
		final Converter<?, ?> freeToPolyline = convertService.getHandler(free,
			Polyline.class);
		assertTrue(freeToPolyline instanceof PolylineRoiToPolylineConverter);

		// Angle to Polyline (should wrap)
		final Converter<?, ?> angleToPolyline = convertService.getHandler(angle,
			Polyline.class);
		assertTrue(angleToPolyline instanceof PolylineRoiToPolylineConverter);

		// PolylineRoi w/ width to Polyline (shouldn't work)
		poly.setStrokeWidth(15.5);
		final Converter<?, ?> widePrToPolyline = convertService.getHandler(poly,
			Polyline.class);
		assertTrue(widePrToPolyline == null);

		// PolylineRoi spline fit to Polyline (shouldn't work)
		poly.setStrokeWidth(0);
		poly.fitSpline();
		final Converter<?, ?> splinePrToPolyline = convertService.getHandler(poly,
			Polyline.class);
		assertTrue(splinePrToPolyline == null);
	}

	@Test
	public void testPolylineRoiToPolyline() {
		final Polyline converted = convertService.convert(poly, Polyline.class);

		assertTrue(converted instanceof PolylineRoiWrapper);

		final float[] xp = poly.getFloatPolygon().xpoints;
		final float[] yp = poly.getFloatPolygon().ypoints;
		assertEquals(poly.getNCoordinates(), converted.numVertices());
		for (int i = 0; i < poly.getNCoordinates(); i++) {
			assertEquals(xp[i], converted.vertex(i).getDoublePosition(0), 0);
			assertEquals(yp[i], converted.vertex(i).getDoublePosition(1), 0);
		}
	}

	@Test
	public void testFreeLineToPolyline() {
		final Polyline converted = convertService.convert(free, Polyline.class);

		assertTrue(converted instanceof UnmodifiablePolylineRoiWrapper);

		final float[] xp = free.getFloatPolygon().xpoints;
		final float[] yp = free.getFloatPolygon().ypoints;
		assertEquals(free.getNCoordinates(), converted.numVertices());
		for (int i = 0; i < free.getNCoordinates(); i++) {
			assertEquals(xp[i], converted.vertex(i).getDoublePosition(0), 0);
			assertEquals(yp[i], converted.vertex(i).getDoublePosition(1), 0);
		}
	}

	@Test
	public void testAngleToPolyline() {
		final Polyline converted = convertService.convert(angle, Polyline.class);

		assertTrue(converted instanceof UnmodifiablePolylineRoiWrapper);

		final float[] xp = angle.getFloatPolygon().xpoints;
		final float[] yp = angle.getFloatPolygon().ypoints;
		assertEquals(angle.getNCoordinates(), converted.numVertices());
		for (int i = 0; i < angle.getNCoordinates(); i++) {
			assertEquals(xp[i], converted.vertex(i).getDoublePosition(0), 0);
			assertEquals(yp[i], converted.vertex(i).getDoublePosition(1), 0);
		}
	}

	@Test
	public void testWrapperToPolyline() {
		final Polyline converted = convertService.convert(wrapPolyline,
			Polyline.class);
		assertTrue(converted == polyline);
	}

	// -- To PolygonRoi converter tests --

	@Test
	public void testPolylineToPolylineRoiConverterMatching() {
		// WritablePolyline to PolygonRoi (should wrap)
		final Converter<?, ?> p = convertService.getHandler(polyline,
			PolygonRoi.class);
		assertTrue(p instanceof WritablePolylineToPolylineRoiConverter);

		// Read only Polyline to PolygonRoi (shouldn't wrap)
		final Polyline test = new TestPolyline();
		final Converter<?, ?> readOnlyPolylineToPr = convertService.getHandler(test,
			PolygonRoi.class);
		assertTrue(readOnlyPolylineToPr instanceof PolylineToPolylineRoiConverter);

		// Wrapped PolylineRoi to PolygonRoi (should unwrap)
		final Converter<?, ?> wrapP = convertService.getHandler(wrap,
			PolygonRoi.class);
		assertTrue(wrapP instanceof WrapperToPolygonRoiConverter);

		// Wrapped Angle to PolygonRoi (should unwrap)
		final Converter<?, ?> wrapA = convertService.getHandler(angleWrap,
			PolygonRoi.class);
		assertTrue(wrapA instanceof WrapperToPolygonRoiConverter);

		// Wrapped FreeLine to PolygonRoi (should unwrap)
		final Converter<?, ?> wrapF = convertService.getHandler(freeWrap,
			PolygonRoi.class);
		assertTrue(wrapF instanceof WrapperToPolygonRoiConverter);

		// 3D polyline to PolygonRoi (shouldn't work)
		final List<RealPoint> pts2 = new ArrayList<>(3);
		pts2.add(new RealPoint(new double[] { 0, 0, 0 }));
		pts2.add(new RealPoint(new double[] { 10, 10, 10 }));
		pts2.add(new RealPoint(new double[] { 12, 8, 8 }));
		final Polyline ddd = new DefaultWritablePolyline(pts2);
		final Converter<?, ?> cd = convertService.getHandler(ddd, PolygonRoi.class);
		assertNull(cd);
	}

	@Test
	public void testPolylineToPolygonRoi() {
		final Polyline readOnly = new TestPolyline();
		final PolygonRoi converted = convertService.convert(readOnly,
			PolygonRoi.class);
		final float[] xp = converted.getFloatPolygon().xpoints;
		final float[] yp = converted.getFloatPolygon().ypoints;

		assertFalse(converted instanceof PolylineWrapper);
		assertEquals(Roi.POLYLINE, converted.getType());
		assertEquals(readOnly.numVertices(), converted.getNCoordinates());
		for (int i = 0; i < readOnly.numVertices(); i++) {
			assertEquals(readOnly.vertex(i).getDoublePosition(0), xp[i], 0);
			assertEquals(readOnly.vertex(i).getDoublePosition(1), yp[i], 0);
		}
	}

	@Test
	public void testWritablePolylineToPolygonRoi() {
		final PolygonRoi pr = convertService.convert(polyline, PolygonRoi.class);
		final float[] x = pr.getFloatPolygon().xpoints;
		final float[] y = pr.getFloatPolygon().ypoints;

		assertTrue(pr instanceof PolylineWrapper);
		assertEquals(Roi.POLYLINE, pr.getType());
		assertEquals(polyline.numVertices(), pr.getNCoordinates());
		for (int i = 0; i < pr.getNCoordinates(); i++) {
			assertEquals(polyline.vertex(i).getDoublePosition(0), x[i], 0);
			assertEquals(polyline.vertex(i).getDoublePosition(1), y[i], 0);
		}
	}

	@Test
	public void testWrappedPolylineToPolygonRoi() {
		final PolygonRoi pr = convertService.convert(wrap, PolygonRoi.class);
		assertTrue(poly == pr);
		assertEquals(Roi.POLYLINE, pr.getType());
	}

	@Test
	public void testWrappedFreelineToPolygonRoi() {
		final PolygonRoi pr = convertService.convert(freeWrap, PolygonRoi.class);
		assertTrue(free == pr);
		assertEquals(Roi.FREELINE, pr.getType());
	}

	@Test
	public void testWrappedAngleToPolygonRoi() {
		final PolygonRoi pr = convertService.convert(angleWrap, PolygonRoi.class);
		assertTrue(angle == pr);
		assertEquals(Roi.ANGLE, pr.getType());
	}

	// -- Helper classes --
	private static final class TestPolyline implements Polyline {

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
