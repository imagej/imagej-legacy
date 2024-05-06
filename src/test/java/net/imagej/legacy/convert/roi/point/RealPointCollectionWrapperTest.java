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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritableRealPointCollection;
import net.imglib2.roi.geom.real.WritableRealPointCollection;
import net.imglib2.roi.util.RealLocalizableRealPositionable;
import net.imglib2.roi.util.RealLocalizableRealPositionableWrapper;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link RealPointCollectionWrapper}
 *
 * @author Alison Walter
 */
public class RealPointCollectionWrapperTest {

	private WritableRealPointCollection<RealLocalizableRealPositionable> rpc;
	private RealPointCollectionWrapper wrap;

	@Before
	public void setup() {
		final List<RealLocalizableRealPositionable> pts = new ArrayList<>(3);
		pts.add(new RealLocalizableRealPositionableWrapper<>(new RealPoint(
			new double[] { 12, 3 })));
		pts.add(new RealLocalizableRealPositionableWrapper<>(new RealPoint(
			new double[] { 0.25, 6.5 })));
		pts.add(new RealLocalizableRealPositionableWrapper<>(new RealPoint(
			new double[] { -107, 33 })));

		rpc = new DefaultWritableRealPointCollection<>(pts);
		wrap = new RealPointCollectionWrapper(rpc);

		// NB: can't remove points without associated image
		final ImagePlus i = IJ.createImage("Ramp", "8-bit ramp", 128, 128, 1);
		i.setRoi(wrap);
		wrap.setImage(i);
	}

	@Test
	public void testGetters() {
		assertTrue(pointsEqual());
	}

	@Test
	public void testGetSource() {
		assertEquals(rpc, wrap.getSource());
	}

	@Test
	public void testSynchronize() {
		wrap.deleteHandle(12, 3);
		wrap.addPoint(200, 18);

		assertFalse(pointsEqual());

		wrap.synchronize();

		assertTrue(pointsEqual());
	}

	@Test
	public void testGetUpdatedSource() {
		wrap.deleteHandle(12, 3);
		wrap.addPoint(0, 0);

		assertFalse(pointsEqual());

		final WritableRealPointCollection<?> wrpc = wrap.getUpdatedSource();

		assertEquals(rpc.hashCode(), wrpc.hashCode());
		assertTrue(pointsEqual());
	}

	@Test
	public void testAddPoint() {
		wrap.addPoint(22, 46);

		final int initialNumPointsRPC = countPointsRPC();
		wrap.synchronize();

		final int modifiedNumPointsRPC = countPointsRPC();
		assertTrue(modifiedNumPointsRPC == initialNumPointsRPC + 1);
		assertTrue(pointsEqual());
	}

	@Test
	public void testRemovePoint() {
		wrap.deleteHandle(-107, 33);

		final int initialNumPointsRPC = countPointsRPC();
		wrap.synchronize();

		final int modifiedNumPointsRPC = countPointsRPC();
		assertTrue(modifiedNumPointsRPC == initialNumPointsRPC - 1);
		assertTrue(pointsEqual());
	}

	// -- Helper methods --

	private boolean pointsEqual() {
		final int numPoints = wrap.getFloatPolygon().npoints;
		final float[] xp = wrap.getFloatPolygon().xpoints;
		final float[] yp = wrap.getFloatPolygon().ypoints;

		int count = 0;
		final Iterator<RealLocalizableRealPositionable> itr = rpc.points()
			.iterator();
		while (itr.hasNext()) {
			final RealLocalizable pt = itr.next();
			boolean match = false;
			for (int i = 0; i < numPoints; i++) {
				if (xp[i] == pt.getDoublePosition(0) && yp[i] == pt.getDoublePosition(
					1))
				{
					match = true;
					count++;
					break;
				}
			}
			if (!match) return false;
		}

		return numPoints == count;
	}

	private int countPointsRPC() {
		final Iterator<?> itr = rpc.points().iterator();
		int count = 0;
		while (itr.hasNext()) {
			itr.next();
			count++;
		}

		return count;
	}

}
