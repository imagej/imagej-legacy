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

package net.imagej.legacy.convert.roi.point;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatPolygon;

import net.imglib2.roi.geom.real.DefaultWritablePointMask;
import net.imglib2.roi.geom.real.WritablePointMask;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link PointMaskWrapper}
 *
 * @author Alison Walter
 */
public class PointMaskWrapperTest {

	private WritablePointMask pm;
	private PointMaskWrapper w;

	@Before
	public void setup() {
		pm = new DefaultWritablePointMask(new double[] { 0.25, 6 });
		w = new PointMaskWrapper(pm);

		// NB: can't remove points without associated image
		final ImagePlus i = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
		i.setRoi(w);
		w.setImage(i);
	}

	@Test
	public void testGetters() {
		final FloatPolygon fp = w.getFloatPolygon();
		assertEquals(1, fp.npoints);
		assertEquals(pm.getDoublePosition(0), fp.xpoints[0], 0);
		assertEquals(pm.getDoublePosition(1), fp.ypoints[0], 0);
	}

	@Test
	public void testGetSource() {
		assertEquals(pm, w.getSource());
	}

	@Test
	public void testSynchronize() {
		w.addPoint(3.5, 22);
		w.deleteHandle(0.25, 6);
		final FloatPolygon fp = w.getFloatPolygon();

		assertNotEquals(pm.getDoublePosition(0), fp.xpoints[0], 0);
		assertNotEquals(pm.getDoublePosition(1), fp.ypoints[0], 0);

		w.synchronize();

		assertEquals(pm.getDoublePosition(0), fp.xpoints[0], 0);
		assertEquals(pm.getDoublePosition(1), fp.ypoints[0], 0);
	}

	@Test
	public void testGetUpdatedSource() {
		w.addPoint(100, 100);
		w.deleteHandle(0.25, 6);

		assertEquals(0.25, pm.getDoublePosition(0), 0);
		assertEquals(6, pm.getDoublePosition(1), 0);

		final WritablePointMask wpm = w.getUpdatedSource();

		assertEquals(pm, wpm);
		assertEquals(100, pm.getDoublePosition(0), 0);
		assertEquals(100, pm.getDoublePosition(1), 0);
	}

	@Test
	public void testAddPoint() {
		w.addPoint(0, 0);
		w.synchronize();

		// Shouldn't affect PointMask, and PointMask still only has one point
		assertEquals(0.25, pm.getDoublePosition(0), 0);
		assertEquals(6, pm.getDoublePosition(1), 0);
	}

	@Test
	public void testRemovePoint() {
		w.deleteHandle(0.25, 6);
		w.synchronize();

		// Shouldn't affect PointMask, and PointMask still only has one point
		assertEquals(0.25, pm.getDoublePosition(0), 0);
		assertEquals(6, pm.getDoublePosition(1), 0);
	}

}
