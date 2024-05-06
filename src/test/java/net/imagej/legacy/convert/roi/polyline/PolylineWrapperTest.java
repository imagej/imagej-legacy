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
import static org.junit.Assert.assertTrue;

import ij.IJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritablePolyline;
import net.imglib2.roi.geom.real.WritablePolyline;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link PolylineWrapper}
 *
 * @author Alison Walter
 */
public class PolylineWrapperTest {

	private WritablePolyline polyline;
	private PolylineWrapper wrap;

	@Before
	public void setup() {
		final List<RealLocalizable> pts = new ArrayList<>(4);
		pts.add(new RealPoint(new double[] { 0, 0 }));
		pts.add(new RealPoint(new double[] { 10, 10 }));
		pts.add(new RealPoint(new double[] { 20, 0 }));
		pts.add(new RealPoint(new double[] { 30, 10 }));
		polyline = new DefaultWritablePolyline(pts);

		wrap = new PolylineWrapper(polyline);

		// NB: can't remove points without associated image
		final ImagePlus i = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
		i.setRoi(wrap);
		wrap.setImage(i);
	}

	@Test
	public void testGetters() {
		assertEquals(polyline.numVertices(), wrap.getFloatPolygon().npoints);

		final float[] xp = wrap.getFloatPolygon().xpoints;
		final float[] yp = wrap.getFloatPolygon().ypoints;
		for (int i = 0; i < polyline.numVertices(); i++) {
			assertEquals(polyline.vertex(i).getDoublePosition(0), xp[i], 0);
			assertEquals(polyline.vertex(i).getDoublePosition(1), yp[i], 0);
		}
	}

	@Test
	public void testGetSource() {
		assertEquals(polyline, wrap.getSource());
	}

	@Test
	public void testSynchronize() {
		wrap.deleteHandle(20, 0);
		assertTrue(polyline.numVertices() - 1 == wrap.getFloatPolygon().npoints);

		wrap.synchronize();

		assertEquals(polyline.numVertices(), wrap.getFloatPolygon().npoints);
		final float[] xp = wrap.getFloatPolygon().xpoints;
		final float[] yp = wrap.getFloatPolygon().ypoints;
		for (int i = 0; i < polyline.numVertices(); i++) {
			assertEquals(polyline.vertex(i).getDoublePosition(0), xp[i], 0);
			assertEquals(polyline.vertex(i).getDoublePosition(1), yp[i], 0);
		}
	}

	@Test
	public void testGetUpdatedSource() {
		wrap.deleteHandle(20, 0);
		assertTrue(polyline.numVertices() - 1 == wrap.getFloatPolygon().npoints);

		final WritablePolyline s = wrap.getUpdatedSource();

		assertEquals(polyline, s);
		assertEquals(polyline.numVertices(), wrap.getFloatPolygon().npoints);
		final float[] xp = wrap.getFloatPolygon().xpoints;
		final float[] yp = wrap.getFloatPolygon().ypoints;
		for (int i = 0; i < polyline.numVertices(); i++) {
			assertEquals(polyline.vertex(i).getDoublePosition(0), xp[i], 0);
			assertEquals(polyline.vertex(i).getDoublePosition(1), yp[i], 0);
		}
	}

}
