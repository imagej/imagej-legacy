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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritableRealPointCollection;
import net.imglib2.roi.geom.real.RealPointCollection;
import net.imglib2.roi.geom.real.WritableRealPointCollection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests {@link PointRoiWrapper}
 *
 * @author Alison Walter
 */
public class PointRoiWrapperTest {

	private PointRoi point;
	private RealPointCollection<RealLocalizable> rpc;
	private WritableRealPointCollection<RealLocalizable> wrap;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		point = new PointRoi(new float[] { 12.125f, 17, 1 }, new float[] { -4, 6.5f,
			30 });
		final List<RealLocalizable> c = new ArrayList<>();
		c.add(new RealPoint(new double[] { 12.125, -4 }));
		c.add(new RealPoint(new double[] { 17, 6.5 }));
		c.add(new RealPoint(new double[] { 1, 30 }));
		rpc = new DefaultWritableRealPointCollection<>(c);
		wrap = new PointRoiWrapper(point);
	}

	// -- PointRoiWrapper --

	@Test
	public void testPointRoiWrapperGetters() {
		Iterator<RealLocalizable> iw = wrap.points().iterator();
		final Iterator<RealLocalizable> irpc = rpc.points().iterator();
		final float[] x = point.getContainedFloatPoints().xpoints;
		final float[] y = point.getContainedFloatPoints().ypoints;

		// Test ImageJ 1.x and wrapper equivalent
		for (int i = 0; i < 3; i++) {
			final RealLocalizable r = iw.next();
			assertEquals(x[i], r.getFloatPosition(0), 0);
			assertEquals(y[i], r.getFloatPosition(1), 0);
		}

		// Test ImgLib2 and wrapper equivalent
		iw = wrap.points().iterator();
		while (irpc.hasNext()) {
			final RealLocalizable w = iw.next();
			final RealLocalizable pc = irpc.next();
			assertEquals(pc.getFloatPosition(0), w.getFloatPosition(0), 0);
			assertEquals(pc.getFloatPosition(1), w.getFloatPosition(1), 0);
		}
	}

	@Test
	public void testPointRoiWrapperAddPoint() {
		wrap.addPoint(new RealPoint(new double[] { -2.25, 13 }));
		final Iterator<RealLocalizable> iw = wrap.points().iterator();
		final RealLocalizable one = iw.next();
		final RealLocalizable two = iw.next();
		final float[] xp = point.getContainedFloatPoints().xpoints;
		final float[] yp = point.getContainedFloatPoints().ypoints;

		assertEquals(xp[0], one.getFloatPosition(0), 0);
		assertEquals(yp[0], one.getFloatPosition(1), 0);
		assertEquals(xp[1], two.getFloatPosition(0), 0);
		assertEquals(yp[1], two.getFloatPosition(1), 0);
	}

	@Test
	public void testPointRoiWrapperRemovePointNoImagePlus() {
		// Throw an exception since wrapped roi has no associated ImagePlus
		exception.expect(UnsupportedOperationException.class);
		wrap.removePoint(new RealPoint(new double[] { 1, 1, }));
	}

	@Test
	public void testPointRoiWrapperRemovePointWithImagePlus() {
		final ImagePlus i = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
		i.setRoi(point);
		point.setImage(i);

		wrap.removePoint(new RealPoint(new double[] { 17, 6.5 }));
		Iterator<RealLocalizable> iw = wrap.points().iterator();
		RealLocalizable one = iw.next();
		RealLocalizable two = iw.next();
		float[] x = point.getContainedFloatPoints().xpoints;
		float[] y = point.getContainedFloatPoints().ypoints;

		// Since the passed point is part of the collection, it should have been
		// removed
		assertEquals(x[0], one.getDoublePosition(0), 0);
		assertEquals(y[0], one.getDoublePosition(1), 0);
		assertEquals(x[1], two.getDoublePosition(0), 0);
		assertEquals(y[1], two.getDoublePosition(1), 0);
		assertEquals(point.getNCoordinates(), 2);
		assertFalse(iw.hasNext());

		wrap.removePoint(new RealPoint(new double[] { 11, 3 }));
		iw = wrap.points().iterator();
		one = iw.next();
		two = iw.next();
		x = point.getContainedFloatPoints().xpoints;
		y = point.getContainedFloatPoints().ypoints;

		// Point was not part of the collection, so no change
		assertEquals(x[0], one.getDoublePosition(0), 0);
		assertEquals(y[0], one.getDoublePosition(1), 0);
		assertEquals(x[1], two.getDoublePosition(0), 0);
		assertEquals(y[1], two.getDoublePosition(1), 0);
		assertEquals(point.getNCoordinates(), 2);
		assertFalse(iw.hasNext());
	}

	@Test
	public void testPointRoiWrapperTest() {
		assertTrue(wrap.test(new RealPoint(new double[] { 12.125, -4 })));
		assertFalse(wrap.test(new RealPoint(new double[] { 8, 15.5 })));
	}

	@Test
	public void testPointRoiWrapperBounds() {
		assertEquals(1, wrap.realMin(0), 0);
		assertEquals(-4, wrap.realMin(1), 0);
		assertEquals(17, wrap.realMax(0), 0);
		assertEquals(30, wrap.realMax(1), 0);
	}

	@Test
	public void testUpdatedAfterPointRoiWrapperModified() {
		final RealPoint remove = new RealPoint(new double[] { 12.125, -4 });
		final RealPoint add = new RealPoint(new double[] { 8, 100.25 });
		assertTrue(wrap.test(remove));
		assertFalse(wrap.test(add));

		// addPoint
		wrap.addPoint(add);
		assertTrue(wrap.test(add));
		assertEquals(100.25, wrap.realMax(1), 0);

		// removePoint
		final ImagePlus i = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
		i.setRoi(point);
		point.setImage(i); // wrapper needs associated ImagePlus in order to
		// removePoint
		wrap.removePoint(remove);
		assertFalse(wrap.test(remove));

		// check the points
		final Iterator<RealLocalizable> iw = wrap.points().iterator();
		final float[] x = point.getContainedFloatPoints().xpoints;
		final float[] y = point.getContainedFloatPoints().ypoints;

		for (int n = 0; n < point.getNCoordinates(); n++) {
			final RealLocalizable pt = iw.next();
			assertEquals(x[n], pt.getDoublePosition(0), 0);
			assertEquals(y[n], pt.getDoublePosition(1), 0);
		}
		assertFalse(iw.hasNext());
	}

}
