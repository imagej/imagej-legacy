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

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritablePolygon2D;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.roi.geom.real.WritablePolygon2D;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests {@link PolygonRoiWrapper}
 *
 * @author Alison Walter
 */
public class PolygonRoiWrapperTest {

	private PolygonRoi poly;
	private WritablePolygon2D wrap;
	private Polygon2D dp;
	private RealLocalizable inside;
	private RealLocalizable outside;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		poly = new PolygonRoi(new float[] { 100.5f, 100.5f, 150, 199, 199 },
			new float[] { 100, 200, 250.25f, 200, 100 }, Roi.POLYGON);
		wrap = new PolygonRoiWrapper(poly);
		dp = new DefaultWritablePolygon2D(new double[] { 100.5, 100.5, 150, 199,
			199 }, new double[] { 100, 200, 250.25, 200, 100 });

		inside = new RealPoint(new double[] { 151, 225 });
		outside = new RealPoint(new double[] { 100, 100 });
	}

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
		wrap.addVertex(3, new RealPoint(0, 0));
	}

	@Test
	public void testPolygonRoiWrapperRemoveVertexNoImagePlus() {
		exception.expect(UnsupportedOperationException.class);
		wrap.removeVertex(0);
	}

	@Test
	public void testPolygonRoiWrapperRemoveVertexWithImagePlus() {
		final ImagePlus i = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
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
	public void testUpdatedAfterPolygonRoiWrapperModified() {
		final ImagePlus i = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
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

}
