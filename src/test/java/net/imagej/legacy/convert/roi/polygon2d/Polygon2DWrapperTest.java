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
import static org.junit.Assert.assertTrue;

import ij.IJ;
import ij.ImagePlus;

import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.WritablePolygon2D;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link Polygon2DWrapper}
 *
 * @author Alison Walter
 */
public class Polygon2DWrapperTest {

	private WritablePolygon2D polygon;
	private Polygon2DWrapper wrap;

	@Before
	public void setup() {
		polygon = new DefaultWritablePolygon2D(new double[] { 1, 4, 7, 4 },
			new double[] { 1, 13, 1, -13 });
		wrap = new Polygon2DWrapper(polygon);

		// NB: can't remove points without associated image
		final ImagePlus i = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
		i.setRoi(wrap);
		wrap.setImage(i);
	}

	@Test
	public void testGetters() {
		final float[] xp = wrap.getFloatPolygon().xpoints;
		final float[] yp = wrap.getFloatPolygon().ypoints;
		assertEquals(polygon.numVertices(), wrap.getFloatPolygon().npoints);
		for(int i = 0; i < polygon.numVertices(); i++) {
			assertEquals(polygon.vertex(i).getDoublePosition(0), xp[i], 0);
			assertEquals(polygon.vertex(i).getDoublePosition(1), yp[i], 0);
		}
	}

	@Test
	public void testGetSource() {
		assertEquals(polygon, wrap.getSource());
	}

	@Test
	public void testSynchronize() {
		wrap.deleteHandle(4, -13);
		assertTrue(polygon.numVertices() - 1 == wrap.getFloatPolygon().npoints);

		wrap.synchronize();
		final float[] xp = wrap.getFloatPolygon().xpoints;
		final float[] yp = wrap.getFloatPolygon().ypoints;
		assertTrue(polygon.numVertices() == wrap.getFloatPolygon().npoints);
		for(int i = 0; i < polygon.numVertices(); i++) {
			assertEquals(polygon.vertex(i).getDoublePosition(0), xp[i], 0);
			assertEquals(polygon.vertex(i).getDoublePosition(1), yp[i], 0);
		}
	}

	@Test
	public void testGetUpdatedSource() {
		wrap.deleteHandle(4, -13);
		assertTrue(polygon.numVertices() - 1 == wrap.getFloatPolygon().npoints);

		final Polygon2D p = wrap.getUpdatedSource();

		assertEquals(polygon, p);
		final float[] xp = wrap.getFloatPolygon().xpoints;
		final float[] yp = wrap.getFloatPolygon().ypoints;
		assertTrue(polygon.numVertices() == wrap.getFloatPolygon().npoints);
		for(int i = 0; i < polygon.numVertices(); i++) {
			assertEquals(polygon.vertex(i).getDoublePosition(0), xp[i], 0);
			assertEquals(polygon.vertex(i).getDoublePosition(1), yp[i], 0);
		}
	}

}
