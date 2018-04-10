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

package net.imagej.legacy.convert.roi.polygon2d;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToPolygonRoiConverter;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.WritablePolygon2D;

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
 * Tests converting {@link PolygonRoi} to {@link Polygon2D} and the
 * corresponding wrappers.
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
	private Polygon2D dp;
	private RealLocalizable inside;
	private RealLocalizable outside;
	private ConvertService convertService;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		poly = new PolygonRoi(new float[] { 100.5f, 100.5f, 150, 199, 199 },
			new float[] { 100, 200, 250.25f, 200, 100 }, Roi.POLYGON);
		wrap = new PolygonRoiWrapper(poly);
		dp = new DefaultWritablePolygon2D(new double[] { 100.5, 100.5, 150, 199,
			199 }, new double[] { 100, 200, 250.25, 200, 100 });

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

		inside = new RealPoint(new double[] { 151, 225 });
		outside = new RealPoint(new double[] { 100, 100 });

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- Wrapper tests --

	@Test
	public void testPolygonRoiWrapperGetters() {
		assertEquals(5, wrap.numVertices());
		final float[] x = poly.getFloatPolygon().xpoints;
		final float[] y = poly.getFloatPolygon().ypoints;

		for (int i = 0; i < 5; i++) {
			// compare ImageJ 1.x to Wrapper
			assertEquals(x[i], wrap.vertex(i).getDoublePosition(0), 0);
			assertEquals(y[i], wrap.vertex(i).getDoublePosition(1), 0);

			// compare ImgLib2 to Wrapper
			assertEquals(dp.vertex(i).getDoublePosition(0), wrap.vertex(i)
				.getDoublePosition(0), 0);
			assertEquals(dp.vertex(i).getDoublePosition(1), wrap.vertex(i)
				.getDoublePosition(1), 0);
		}
	}

	@Test
	public void testPolygonRoiWrapperSetVertex() {
		exception.expect(UnsupportedOperationException.class);
		wrap.vertex(2).setPosition(new double[] { 1, -3 });
	}

	@Test
	public void testPolygonRoiWrapperAddVertex() {
		exception.expect(UnsupportedOperationException.class);
		wrap.addVertex(3, new double[] { 0, 0 });
	}

	@Test
	public void testPolygonRoiWrapperRemoveVertexNoImagePlus() {
		exception.expect(UnsupportedOperationException.class);
		wrap.removeVertex(0);
	}

	@Test
	public void testPolygonRoiWrapperRemoveVertexWithImagePlus() {
		final ImagePlus i = new ImagePlus("http://imagej.net/images/blobs.gif");
		i.setRoi(poly);
		poly.setImage(i);

		wrap.removeVertex(2);
		assertEquals(4, wrap.numVertices());

		// Check that backing PolygonRoi was updated
		assertEquals(4, poly.getNCoordinates());
	}

	@Test
	public void testPolygonRoiWrapperTest() {
		assertTrue(wrap.test(inside));
		assertFalse(wrap.test(outside));
	}

	@Test
	public void testPolygonRoiWrapperBounds() {
		assertEquals(100.5, wrap.realMin(0), 0);
		assertEquals(100, wrap.realMin(1), 0);
		assertEquals(199, wrap.realMax(0), 0);
		assertEquals(250.25, wrap.realMax(1), 0);
	}

	@Test
	public void testUnmodifiablePolygonRoiWrapperFreeRoiGetters() {
		final RealLocalizable twentytwo = freeWrap.vertex(22);
		assertEquals(2, twentytwo.getDoublePosition(0), 0);
		assertEquals(7, twentytwo.getDoublePosition(1), 0);

		assertEquals(free.getNCoordinates(), freeWrap.numVertices());
	}

	@Test
	public void testUnmodifiablePolygonRoiWrapperFreeRoiTest() {
		assertTrue(freeWrap.test(new RealPoint(new double[] { 16.25, 13 })));
		assertTrue(freeWrap.test(new RealPoint(new double[] { 15, 1.125 })));
		assertFalse(freeWrap.test(new RealPoint(new double[] { 27.25, 8 })));
	}

	@Test
	public void testUnmodifiablePolygonRoiWrapperFreeRoiBounds() {
		assertEquals(0, freeWrap.realMin(0), 0);
		assertEquals(0, freeWrap.realMin(1), 0);
		assertEquals(27, freeWrap.realMax(0), 0);
		assertEquals(23, freeWrap.realMax(1), 0);
	}

	@Test
	public void testUnmodifiablePolygonRoiWrapperTracedRoiGetters() {
		final RealLocalizable three = tracedWrap.vertex(3);
		assertEquals(0, three.getDoublePosition(0), 0);
		assertEquals(5, three.getDoublePosition(1), 0);

		assertEquals(traced.getNCoordinates(), tracedWrap.numVertices());
	}

	@Test
	public void testUnmodifiablePolygonRoiWrapperTracedRoiTest() {
		assertTrue(tracedWrap.test(new RealPoint(new double[] { 6.5, 1.125 })));
		assertTrue(tracedWrap.test(new RealPoint(new double[] { 1.5, 4 })));
		assertFalse(tracedWrap.test(new RealPoint(new double[] { 8, 5 })));
	}

	@Test
	public void testUnmodifiablePolygonRoiWrapperTracedRoiBounds() {
		assertEquals(0, tracedWrap.realMin(0), 0);
		assertEquals(0, tracedWrap.realMin(1), 0);
		assertEquals(8, tracedWrap.realMax(0), 0);
		assertEquals(5, tracedWrap.realMax(1), 0);
	}

	@Test
	public void testUpdatedAfterPolygonRoiWrapperModified() {
		final ImagePlus i = new ImagePlus("http://imagej.net/images/blobs.gif");
		i.setRoi(poly);
		poly.setImage(i);

		assertTrue(wrap.test(inside));
		assertFalse(wrap.test(outside));

		wrap.removeVertex(2);
		assertFalse(wrap.test(inside));
		assertFalse(wrap.test(outside));

		// Check bounds updated
		assertEquals(100.5, wrap.realMin(0), 0);
		assertEquals(100, wrap.realMin(1), 0);
		assertEquals(199, wrap.realMax(0), 0);
		assertEquals(200, wrap.realMax(1), 0);
	}

	// -- to Polygon2D conversion tests --

	@Test
	public void testPolygonRoiToPolygon2DConverter() {
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
	public void testPolygonRoiToPolygon2DConverterFreeRoi() {
		final Polygon2D converted = convertService.convert(free, Polygon2D.class);
		assertTrue(converted instanceof UnmodifiablePolygonRoiWrapper);
	}

	@Test
	public void testPolygonRoiToPolygon2DConverterTracedRoi() {
		final Polygon2D converted = convertService.convert(traced, Polygon2D.class);
		assertTrue(converted instanceof UnmodifiablePolygonRoiWrapper);
	}

	@Test
	public void testPolygonRoiToPolygon2DConverterWithSpline() {
		poly.fitSpline();
		final Polygon2D converted = convertService.convert(poly, Polygon2D.class);

		assertTrue(converted == null);
	}

	// -- to PolygonRoi conversion tests --

	@Test
	public void testPolygonRoiConverterMatching() {
		final Polygon2D py = new DefaultWritablePolygon2D(new double[] { 10, 10, 15,
			20, 20 }, new double[] { 1, 20, 33, 20, 1 });
		final Converter<?, ?> c = convertService.getHandler(py, PolygonRoi.class);
		assertTrue(c instanceof Polygon2DToPolygonRoiConverter);

		final Converter<?, ?> pWrap = convertService.getHandler(wrap,
			PolygonRoi.class);
		assertTrue(pWrap instanceof WrapperToPolygonRoiConverter);

		final Converter<?, ?> tWrap = convertService.getHandler(tracedWrap,
			PolygonRoi.class);
		assertTrue(tWrap instanceof WrapperToPolygonRoiConverter);

		final Converter<?, ?> fWrap = convertService.getHandler(freeWrap,
			PolygonRoi.class);
		assertTrue(fWrap instanceof WrapperToPolygonRoiConverter);
	}

	@Test
	public void testPolygon2DToPolygonRoiConversion() {
		final Polygon2D py = new DefaultWritablePolygon2D(new double[] { 10, 10, 15,
			20, 20 }, new double[] { 1, 20, 33, 20, 1 });
		final PolygonRoi pr = convertService.convert(py, PolygonRoi.class);

		assertEquals(Roi.POLYGON, pr.getType());
		assertEquals(py.numVertices(), pr.getNCoordinates());
		final float[] x = pr.getFloatPolygon().xpoints;
		final float[] y = pr.getFloatPolygon().ypoints;
		for (int i = 0; i < py.numVertices(); i++) {
			final RealLocalizable v = py.vertex(i);
			assertEquals(v.getDoublePosition(0), x[i], 0);
			assertEquals(v.getDoublePosition(1), y[i], 0);
		}
	}

	@Test
	public void testWrappedPolygonToPolygonRoiConversion() {
		final PolygonRoi pr = convertService.convert(wrap, PolygonRoi.class);
		assertEquals(Roi.POLYGON, pr.getType());
		assertTrue(poly == pr);
	}

	@Test
	public void testWrappedFreelineToPolygonRoiConversion() {
		final PolygonRoi pr = convertService.convert(freeWrap, PolygonRoi.class);
		assertEquals(Roi.FREEROI, pr.getType());
		assertTrue(free == pr);
	}

	@Test
	public void testWrappedTracedToPolygonRoiConversion() {
		final PolygonRoi pr = convertService.convert(tracedWrap, PolygonRoi.class);
		assertEquals(Roi.TRACED_ROI, pr.getType());
		assertTrue(traced == pr);
	}
}
