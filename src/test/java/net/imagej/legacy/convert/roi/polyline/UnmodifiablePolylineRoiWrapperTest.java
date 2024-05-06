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

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.Polyline;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests {@link UnmodifiablePolylineRoiWrapper}
 *
 * @author Alison Walter
 */
public class UnmodifiablePolylineRoiWrapperTest {

	private PolygonRoi free;
	private Polyline freeWrap;
	private PolygonRoi angle;
	private Polyline angleWrap;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
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
	}

	// -- FreeLine --

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

	// -- Angle --

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

}
