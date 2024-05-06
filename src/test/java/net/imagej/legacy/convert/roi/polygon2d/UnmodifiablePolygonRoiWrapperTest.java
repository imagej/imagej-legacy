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

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.Polygon2D;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link UnmodifiablePolygonRoiWrapper}
 *
 * @author Alison Walter
 */
public class UnmodifiablePolygonRoiWrapperTest {

	private PolygonRoi free;
	private Polygon2D freeWrap;
	private PolygonRoi traced;
	private Polygon2D tracedWrap;

	@Before
	public void setup() {
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
	}

	// -- FreeRoi --

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

	// -- TracedRoi --

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

}
