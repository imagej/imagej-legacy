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
import static org.junit.Assert.assertTrue;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

import net.imglib2.Point;
import net.imglib2.RealPoint;
import net.imglib2.roi.RealMaskRealInterval;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link IrregularPolylineRoiWrapper}
 *
 * @author Alison Walter
 */
public class IrregularPolylineRoiWrapperTest {

	private PolygonRoi poly;
	private RealMaskRealInterval wrap;
	private PolygonRoi free;
	private RealMaskRealInterval freeWrap;

	@Before
	public void setup() {
		// 53, -27
		poly = new PolygonRoi(new float[] { 1.25f, 20, 50, 79 }, new float[] {
			1.25f, 20, -30, -1 }, Roi.POLYLINE);
		poly.updateWideLine(0);
		wrap = new IrregularPolylineRoiWrapper(poly);

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
		free.updateWideLine(0);
		freeWrap = new IrregularPolylineRoiWrapper(free);
	}

	@Test
	public void testIrregularPolylineRoiWrapperPolylineWithWidth() {
		poly.updateWideLine(4);

		// Test test(...)
		final Point test = new Point(new int[] { 23, 15 });
		assertTrue(wrap.test(test));

		test.setPosition(new int[] { 7, 5 });
		assertTrue(wrap.test(test));

		test.setPosition(new int[] { 52, -26 });
		assertTrue(wrap.test(test));

		test.setPosition(new int[] { 51, -25 });
		assertFalse(wrap.test(test));

		// Test bounds
		assertEquals(1.25 - 2, wrap.realMin(0), 0);
		assertEquals(-30 - 2, wrap.realMin(1), 0);
		assertEquals(79 + 2, wrap.realMax(0), 0);
		assertEquals(20 + 2, wrap.realMax(1), 0);
	}

	@Test
	public void testIrregularPolylineRoiWrapperFreelineWithWidth() {
		free.updateWideLine(10.5f);

		// Test test(...)
		final Point test = new Point(new int[] { 115, 50 });
		assertTrue(freeWrap.test(test));

		test.setPosition(new int[] { 143, 110 });
		assertTrue(freeWrap.test(test));

		test.setPosition(new int[] { 144, 96 });
		assertTrue(freeWrap.test(test));

		test.setPosition(new int[] { 120, 25 });
		assertFalse(freeWrap.test(test));

		// Test bounds
		assertEquals(115 - 5.25, freeWrap.realMin(0), 0);
		assertEquals(34 - 5.25, freeWrap.realMin(1), 0);
		assertEquals(184 + 5.25, freeWrap.realMax(0), 0);
		assertEquals(130 + 5.25, freeWrap.realMax(1), 0);
	}

	@Test
	public void testIrregularPolylineRoiWrapperSplineFitPolyline() {
		poly.fitSpline();
		final float[] x = poly.getFloatPolygon().xpoints;
		final float[] y = poly.getFloatPolygon().ypoints;

		// Test test(...)
		// It is difficult to guess points along the spline, so this just tests
		// that spline points are considered "contained" by the polyline
		final RealPoint test = new RealPoint(new double[] { x[0], y[0] });
		assertTrue(wrap.test(test));

		test.setPosition(new double[] { x[90], y[90] });
		assertTrue(wrap.test(test));

		// contained by non-spline polyline
		test.setPosition(new double[] { 18, 18 });
		assertFalse(wrap.test(test));

		// Test bounds
		// The bounds of the underlying Roi change when the polyline is fit with a
		// spline
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < x.length; i++) {
			if (x[i] < minX) minX = x[i];
			if (y[i] < minY) minY = y[i];
			if (x[i] > maxX) maxX = x[i];
			if (y[i] > maxY) maxY = y[i];
		}

		assertEquals(minX, wrap.realMin(0), 0);
		assertEquals(minY, wrap.realMin(1), 0);
		assertEquals(maxX, wrap.realMax(0), 0);
		assertEquals(maxY, wrap.realMax(1), 0);
	}

	@Test
	public void testIrregularPolylineRoiWrapperSplineFitPolylineWithWidth() {
		poly.updateWideLine(5);
		poly.fitSpline();
		final float[] x = poly.getFloatPolygon().xpoints;
		final float[] y = poly.getFloatPolygon().ypoints;

		final RealPoint test = new RealPoint(new double[] { 17, 20 });
		assertTrue(wrap.test(test));

		test.setPosition(new double[] { 25, 11 });
		assertTrue(wrap.test(test));

		// contained by non-spline polyline
		test.setPosition(new double[] { 18, 18 });
		assertFalse(wrap.test(test));

		// Test bounds
		// The bounds of the underlying Roi change when the polyline is fit with a
		// spline. But changing the width doesn't change the bounds.
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < x.length; i++) {
			if (x[i] < minX) minX = x[i];
			if (y[i] < minY) minY = y[i];
			if (x[i] > maxX) maxX = x[i];
			if (y[i] > maxY) maxY = y[i];
		}

		assertEquals(minX - 2.5, wrap.realMin(0), 0);
		assertEquals(minY - 2.5, wrap.realMin(1), 0);
		assertEquals(maxX + 2.5, wrap.realMax(0), 0);
		assertEquals(maxY + 2.5, wrap.realMax(1), 0);
	}

}
