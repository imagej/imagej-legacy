/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
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

package net.imagej.legacy.convert.roi.polyline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

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

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * Tests converting between {@link PolygonRoi} and {@link Polyline}, and the
 * corresponding wrappers.
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
	private Polyline dp;
	private ConvertService convertService;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		poly = new PolygonRoi(new float[] { 1.25f, 20, 50, 79 }, new float[] {
			1.25f, 20, -30, -1 }, Roi.POLYLINE);
		wrap = new PolylineRoiWrapper(poly);
		final List<RealPoint> pts = new ArrayList<>(4);
		pts.add(new RealPoint(new double[] { 1.25, 1.25 }));
		pts.add(new RealPoint(new double[] { 20, 20 }));
		pts.add(new RealPoint(new double[] { 50, -30 }));
		pts.add(new RealPoint(new double[] { 79, -1 }));
		dp = new DefaultWritablePolyline(pts);

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
		freeWrap = new UnmodifiablePolylineRoiWrapper(free);

		angle = new PolygonRoi(new int[] { 166, 80, 163 }, new int[] { 79, 122,
			126 }, 3, Roi.ANGLE);
		angleWrap = new UnmodifiablePolylineRoiWrapper(angle);

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- Test Wrappers --

	@Test
	public void testPolylineRoiWrapperGetters() {
		assertEquals(4, wrap.numVertices());
		final float[] x = poly.getFloatPolygon().xpoints;
		final float[] y = poly.getFloatPolygon().ypoints;

		for (int i = 0; i < 4; i++) {
			// compare ImageJ 1.x to Wrapper
			assertEquals(x[i], wrap.vertex(i).getDoublePosition(0), 0);
			assertEquals(y[i], wrap.vertex(i).getDoublePosition(1), 0);

			// compare ImgLib2 to Wrapper
			assertEquals(dp.vertex(i).getDoublePosition(1), wrap.vertex(i)
				.getDoublePosition(1), 0);
			assertEquals(dp.vertex(i).getDoublePosition(1), wrap.vertex(i)
				.getDoublePosition(1), 0);
		}
	}

	@Test
	public void testPolylineRoiWrapperSetVertex() {
		exception.expect(UnsupportedOperationException.class);
		wrap.vertex(2).setPosition(new double[] { 1, -3 });
	}

	@Test
	public void testPolylineRoiWrapperAddVertex() {
		exception.expect(UnsupportedOperationException.class);
		wrap.addVertex(3, new RealPoint(new double[] { 0, 0 }));
	}

	@Test
	public void testPolylineRoiWrapperRemoveVertexNoImagePlus() {
		exception.expect(UnsupportedOperationException.class);
		wrap.removeVertex(0);
	}

	@Test
	public void testPolylineRoiWrapperRemoveVertexWithImagePlus() {
		final ImagePlus i = new ImagePlus("http://imagej.net/images/blobs.gif");
		i.setRoi(poly);
		poly.setImage(i);

		wrap.removeVertex(3);
		assertEquals(3, wrap.numVertices());

		// Check that backing PolygonRoi was updated
		assertEquals(3, poly.getNCoordinates());
	}

	@Test
	public void testPolylineRoiWrapperTest() {
		assertTrue(wrap.test(new RealPoint(new double[] { 10, 10 })));
		assertTrue(wrap.test(new RealPoint(new double[] { 35, -5 })));
		assertTrue(wrap.test(new RealPoint(new double[] { 51.25, -28.75 })));
		assertFalse(wrap.test(new RealPoint(new double[] { 0, 0 })));
		assertFalse(wrap.test(new RealPoint(new double[] { 17, 25 })));
	}

	@Test
	public void testPolylineRoiWrapperBounds() {
		assertEquals(1.25, wrap.realMin(0), 0);
		assertEquals(-30, wrap.realMin(1), 0);
		assertEquals(79, wrap.realMax(0), 0);
		assertEquals(20, wrap.realMax(1), 0);
	}

	@Test
	public void testUpdatedAfterPolylineRoiWrapperModified() {
		final ImagePlus i = new ImagePlus("http://imagej.net/images/blobs.gif");
		i.setRoi(poly);
		poly.setImage(i);

		final RealLocalizable test = new RealPoint(new double[] { 51.25, -28.75 });
		assertTrue(wrap.test(test));

		wrap.removeVertex(3);
		assertFalse(wrap.test(test));

		// Check boundaries updated
		assertEquals(1.25, wrap.realMin(0), 0);
		assertEquals(-30, wrap.realMin(1), 0);
		assertEquals(50, wrap.realMax(0), 0);
		assertEquals(20, wrap.realMax(1), 0);
	}

	@Test
	public void testUnmodifiablePolylineRoiWrapperFreelineGetters() {
		final RealLocalizable oneHundredOne = freeWrap.vertex(101);
		assertEquals(141, oneHundredOne.getDoublePosition(0), 0);
		assertEquals(103, oneHundredOne.getDoublePosition(1), 0);

		assertEquals(free.getNCoordinates(), freeWrap.numVertices());
	}

	@Test
	public void testUnmodifiablePolylineRoiWrapperFreelineTest() {
		assertTrue(freeWrap.test(new RealPoint(new double[] { 135, 72 })));
		assertTrue(freeWrap.test(new RealPoint(new double[] { 164, 112 })));
		assertFalse(freeWrap.test(new RealPoint(new double[] { 120, 167 })));
	}

	@Test
	public void testUnmodifiablePolylineRoiWrapperFreelineBounds() {
		assertEquals(115, freeWrap.realMin(0), 0);
		assertEquals(34, freeWrap.realMin(1), 0);
		assertEquals(183, freeWrap.realMax(0), 0);
		assertEquals(129, freeWrap.realMax(1), 0);
	}

	@Test
	public void testUnmodifiablePolylineRoiWrapperAngleGetters() {
		final RealLocalizable one = angleWrap.vertex(0);
		final RealLocalizable two = angleWrap.vertex(1);
		final RealLocalizable three = angleWrap.vertex(2);

		assertEquals(166, one.getDoublePosition(0), 0);
		assertEquals(79, one.getDoublePosition(1), 0);
		assertEquals(80, two.getDoublePosition(0), 0);
		assertEquals(122, two.getDoublePosition(1), 0);
		assertEquals(163, three.getDoublePosition(0), 0);
		assertEquals(126, three.getDoublePosition(1), 0);

		assertEquals(angle.getNCoordinates(), angleWrap.numVertices());
	}

	@Test
	public void testUnmodifiablePolylineRoiWrapperAngleTest() {
		assertTrue(angleWrap.test(new RealPoint(new double[] { 150, 87 })));
		assertTrue(angleWrap.test(new RealPoint(new double[] { 121.5, 124 })));
		assertFalse(angleWrap.test(new RealPoint(new double[] { 59.25, 121 })));
	}

	@Test
	public void testUnmodifiablePolylineRoiWrapperAngleBounds() {
		assertEquals(80, angleWrap.realMin(0), 0);
		assertEquals(79, angleWrap.realMin(1), 0);
		assertEquals(166, angleWrap.realMax(0), 0);
		assertEquals(126, angleWrap.realMax(1), 0);
	}

	// -- To Polyline conversion tests --

	@Test
	public void testPolylineRoiToPolylineConverter() {
		final Polyline converted = convertService.convert(poly, Polyline.class);

		assertTrue(converted instanceof PolylineRoiWrapper);

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
	}

	@Test
	public void testPolylineRoiToPolylineConverterWithFreeLine() {
		final Polyline converted = convertService.convert(free, Polyline.class);

		assertTrue(converted instanceof UnmodifiablePolylineRoiWrapper);
	}

	@Test
	public void testPolylineRoiToPolylineConverterWithAngle() {
		final Polyline converted = convertService.convert(angle, Polyline.class);

		assertTrue(converted instanceof UnmodifiablePolylineRoiWrapper);
	}

	@Test
	public void testPolylineRoiToPolylineConverterWithWidth() {
		poly.setStrokeWidth(15.5);
		final Polyline converted = convertService.convert(poly, Polyline.class);

		assertTrue(converted == null);
	}

	@Test
	public void testPolylineRoiToPolylineConverterWithSpline() {
		poly.fitSpline();
		final Polyline converted = convertService.convert(poly, Polyline.class);

		assertTrue(converted == null);
	}

	// -- To PolygonRoi converter tests --

	@Test
	public void testPolylineToPolylineRoiConverterMatching() {
		final List<RealPoint> pts = new ArrayList<>(4);
		pts.add(new RealPoint(new double[] { 1, 1 }));
		pts.add(new RealPoint(new double[] { 3.25, -6 }));
		pts.add(new RealPoint(new double[] { 10.5, 8.5 }));
		pts.add(new RealPoint(new double[] { 9, 6 }));
		final Polyline pl = new DefaultWritablePolyline(pts);
		final Converter<?, ?> polyline = convertService.getHandler(pl,
			PolygonRoi.class);
		assertTrue(polyline instanceof PolylineToPolylineRoiConverter);

		final Converter<?, ?> wrapP = convertService.getHandler(wrap,
			PolygonRoi.class);
		assertTrue(wrapP instanceof WrapperToPolygonRoiConverter);

		final Converter<?, ?> wrapA = convertService.getHandler(angleWrap,
			PolygonRoi.class);
		assertTrue(wrapA instanceof WrapperToPolygonRoiConverter);

		final Converter<?, ?> wrapF = convertService.getHandler(freeWrap,
			PolygonRoi.class);
		assertTrue(wrapF instanceof WrapperToPolygonRoiConverter);

		final List<RealPoint> pts2 = new ArrayList<>(3);
		pts2.add(new RealPoint(new double[] { 0, 0, 0 }));
		pts2.add(new RealPoint(new double[] { 10, 10, 10 }));
		pts2.add(new RealPoint(new double[] { 12, 8, 8 }));
		final Polyline ddd = new DefaultWritablePolyline(pts2);
		final Converter<?, ?> cd = convertService.getHandler(ddd, PolygonRoi.class);
		assertNull(cd);
	}

	@Test
	public void testPolylineToPolygonRoiConversion() {
		final List<RealPoint> verts = new ArrayList<>(4);
		verts.add(new RealPoint(new double[] { -1.25, 10 }));
		verts.add(new RealPoint(new double[] { 6, -1.25 }));
		verts.add(new RealPoint(new double[] { 107.5, -12 }));
		verts.add(new RealPoint(new double[] { 23.125, 300 }));
		final Polyline pl = new DefaultWritablePolyline(verts);
		final PolygonRoi pr = convertService.convert(pl, PolygonRoi.class);
		final float[] x = pr.getFloatPolygon().xpoints;
		final float[] y = pr.getFloatPolygon().ypoints;

		assertEquals(Roi.POLYLINE, pr.getType());
		assertEquals(pl.numVertices(), pr.getNCoordinates());
		for (int i = 0; i < pr.getNCoordinates(); i++) {
			assertEquals(verts.get(i).getDoublePosition(0), x[i], 0);
			assertEquals(verts.get(i).getDoublePosition(1), y[i], 0);
		}
	}

	@Test
	public void testWrappedPolylineToPolygonRoiConversion() {
		final PolygonRoi pr = convertService.convert(wrap, PolygonRoi.class);
		assertTrue(poly == pr);
		assertEquals(Roi.POLYLINE, pr.getType());
	}

	@Test
	public void testWrappedFreelineToPolygonRoiConversion() {
		final PolygonRoi pr = convertService.convert(freeWrap, PolygonRoi.class);
		assertTrue(free == pr);
		assertEquals(Roi.FREELINE, pr.getType());
	}

	@Test
	public void testWrappedAngleToPolygonRoiConversion() {
		final PolygonRoi pr = convertService.convert(angleWrap, PolygonRoi.class);
		assertTrue(angle == pr);
		assertEquals(Roi.ANGLE, pr.getType());
	}
}
