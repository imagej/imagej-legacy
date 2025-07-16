/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2025 ImageJ2 developers.
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

import ij.IJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.roi.geom.real.DefaultWritableRealPointCollection;
import net.imglib2.roi.geom.real.WritableRealPointCollection;

import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * Tests {@link RealPointCollectionWrapper}
 *
 * @author Alison Walter
 * @author Gabriel Selzer
 */
public class RealPointCollectionWrapperTest {

	private WritableRealPointCollection<RealPoint> rpc;
	private RealPointCollectionWrapper<RealPoint> wrap;

	@Before
	public void setup() {
		final List<RealPoint> pts = new ArrayList<>(3);
		pts.add(new RealPoint(12, 3));
		pts.add(new RealPoint(0.25, 6.5));
		pts.add(new RealPoint(-107, 33));

		rpc = new DefaultWritableRealPointCollection<>(pts);
		wrap = new RealPointCollectionWrapper<>(rpc, () -> new RealPoint(2));

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

	/**
	 * Ensures that {@link WritableRealPointCollection}s can wrap
	 * {@link RealRandomAccess}es (i.e. something that is not a {@link RealPoint})
	 */
	@Test
	public void testRPCOfRandomAccesses() {
		RealRandomAccessible<UnsignedByteType> testImg = Views.interpolate(ArrayImgs
			.unsignedBytes(10, 10), new NearestNeighborInterpolatorFactory<>());
		RealRandomAccess<UnsignedByteType> ra1 = testImg.realRandomAccess();
		ra1.setPosition(new int[] { 1, 1 });
		WritableRealPointCollection<RealRandomAccess<UnsignedByteType>> rpc =
			new DefaultWritableRealPointCollection<>(Collections.singletonList(ra1));

		RealPointCollectionWrapper<RealRandomAccess<UnsignedByteType>> wrapped =
			new RealPointCollectionWrapper<>(rpc, testImg::realRandomAccess);

		// add a new point
		wrapped.addPoint(2.0, 3.0);
		wrapped.synchronize();

		// assert there are now two RRAs
		assertEquals(2, rpc.size());
		Iterator<RealRandomAccess<UnsignedByteType>> iw = rpc.points().iterator();
		RealRandomAccess<UnsignedByteType> ra = iw.next();
		assertArrayEquals(new double[] { 1.0, 1.0 }, ra.positionAsDoubleArray(),
			1e-6);
		ra = iw.next();
		assertArrayEquals(new double[] { 2.0, 3.0 }, ra.positionAsDoubleArray(),
			1e-6);
		assertFalse(iw.hasNext());

	}

	// -- Helper methods --

	private boolean pointsEqual() {
		final int numPoints = wrap.getFloatPolygon().npoints;
		final float[] xp = wrap.getFloatPolygon().xpoints;
		final float[] yp = wrap.getFloatPolygon().ypoints;

		int count = 0;
		final Iterator<RealPoint> itr = rpc.points().iterator();
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
