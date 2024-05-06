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

import ij.gui.PointRoi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.imagej.legacy.convert.roi.MaskPredicateUnwrappers.WrapperToWritablePointMask;
import net.imagej.legacy.convert.roi.MaskPredicateUnwrappers.WrapperToWritableRealPointCollection;
import net.imagej.legacy.convert.roi.MaskPredicateWrapper;
import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToPointRoiConverter;
import net.imglib2.AbstractRealLocalizable;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritablePointMask;
import net.imglib2.roi.geom.real.DefaultWritableRealPointCollection;
import net.imglib2.roi.geom.real.PointMask;
import net.imglib2.roi.geom.real.RealPointCollection;
import net.imglib2.roi.geom.real.WritablePointMask;
import net.imglib2.roi.geom.real.WritableRealPointCollection;
import net.imglib2.roi.util.RealLocalizableRealPositionable;
import net.imglib2.roi.util.RealLocalizableRealPositionableWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting between {@link PointRoi} and {@link RealPointCollection} /
 * {@link PointMask}.
 *
 * @author Alison Walter
 */
public class PointRoiConversionTest {

	private PointRoi point;
	private WritableRealPointCollection<RealLocalizableRealPositionable> rpc;
	private WritableRealPointCollection<RealLocalizable> pointRoiWrap;
	private PointRoi rpcWrap;
	private ConvertService convertService;

	@Before
	public void setup() {
		point = new PointRoi(new float[] { 12.125f, 17, 1 }, new float[] { -4, 6.5f,
			30 });
		final List<RealLocalizableRealPositionable> c = new ArrayList<>();
		c.add(new RealLocalizableRealPositionableWrapper<>(new RealPoint(
			new double[] { 12.125, -4 })));
		c.add(new RealLocalizableRealPositionableWrapper<>(new RealPoint(
			new double[] { 17, 6.5 })));
		c.add(new RealLocalizableRealPositionableWrapper<>(new RealPoint(
			new double[] { 1, 30 })));
		rpc = new DefaultWritableRealPointCollection<>(c);
		pointRoiWrap = new PointRoiWrapper(point);
		rpcWrap = new RealPointCollectionWrapper(rpc);

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- PointRoi To RealPointCollection tests --

	@Test
	public void testPointRoiToRPCConverterMatching() {
		// Single Point PointRoi to RPC (should wrap)
		final PointRoi single = new PointRoi(10, 29);
		final Converter<?, ?> singlePointRoiToRPC = convertService.getHandler(
			single, RealPointCollection.class);
		assertTrue(
			singlePointRoiToRPC instanceof PointRoiToRealPointCollectionConverter);

		// Multiple Points PointRoi to RPC (should wrap)
		final Converter<?, ?> multiPointRoiToRPC = convertService.getHandler(point,
			RealPointCollection.class);
		assertTrue(
			multiPointRoiToRPC instanceof PointRoiToRealPointCollectionConverter);

		// Wrapped RPC to RPC (should unwrap)
		final Converter<?, ?> wrapperToRPC = convertService.getHandler(rpcWrap,
			RealPointCollection.class);
		assertTrue(wrapperToRPC instanceof WrapperToWritableRealPointCollection);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPointRoiToRPC() {
		final RealPointCollection<RealLocalizable> converted = convertService
			.convert(point, RealPointCollection.class);

		assertTrue(converted instanceof PointRoiWrapper);

		final Iterator<RealLocalizable> ic = converted.points().iterator();
		final Iterator<RealLocalizable> iw = pointRoiWrap.points().iterator();
		while (ic.hasNext()) {
			final RealLocalizable wrl = iw.next();
			final RealLocalizable crl = ic.next();
			assertEquals(wrl.getFloatPosition(0), crl.getFloatPosition(0), 0);
			assertEquals(wrl.getFloatPosition(1), crl.getFloatPosition(1), 0);
		}
	}

	@Test
	public void testWrapperToRPC() {
		final RealPointCollection<?> converted = convertService.convert(rpcWrap,
			RealPointCollection.class);
		assertTrue(converted == rpc);
	}

	// -- RealPointCollection to PointRoi tests --

	@Test
	public void testRPCToPointRoiConverterMatching() {
		// Writable RPC to PointRoi (should wrap)
		final Converter<?, ?> c = convertService.getHandler(rpc, PointRoi.class);
		assertTrue(c instanceof WritableRealPointCollectionToPointRoiConverter);

		// Read only RPC to PointRoi (shouldn't wrap)
		final RealPointCollection<?> test = new TestRPC();
		final Converter<?, ?> readOnlyToPointRoi = convertService.getHandler(test,
			PointRoi.class);
		assertTrue(
			readOnlyToPointRoi instanceof RealPointCollectionToPointRoiConverter);

		// Wrapped PointRoi to PointRoi (should unwrap)
		final Converter<?, ?> cc = convertService.getHandler(pointRoiWrap,
			PointRoi.class);
		assertTrue(cc instanceof WrapperToPointRoiConverter);
	}

	@Test
	public void testRPCToPointRoi() {
		final RealPointCollection<RealLocalizable> test = new TestRPC();
		final PointRoi converted = convertService.convert(test, PointRoi.class);

		assertFalse(converted instanceof MaskPredicateWrapper);

		final float[] xp = converted.getFloatPolygon().xpoints;
		final float[] yp = converted.getFloatPolygon().ypoints;
		final Iterator<RealLocalizable> itr = test.points().iterator();
		int count = 0;

		while (itr.hasNext()) {
			final RealLocalizable rl = itr.next();
			assertEquals(rl.getFloatPosition(0), xp[count], 0);
			assertEquals(rl.getFloatPosition(1), yp[count], 0);
			count++;
		}

		assertEquals(count, converted.getNCoordinates());
	}

	@Test
	public void testWritableRPCToPointRoi() {
		final PointRoi p = convertService.convert(rpc, PointRoi.class);

		assertTrue(p instanceof RealPointCollectionWrapper);

		final float[] xp = p.getContainedFloatPoints().xpoints;
		final float[] yp = p.getContainedFloatPoints().ypoints;
		final Iterator<RealLocalizableRealPositionable> points = rpc.points()
			.iterator();
		int count = 0;

		while (points.hasNext()) {
			final RealLocalizable tp = points.next();
			assertEquals(tp.getFloatPosition(0), xp[count], 0);
			assertEquals(tp.getFloatPosition(1), yp[count], 0);
			count++;
		}

		assertEquals(count, p.getNCoordinates());
		assertEquals(rpc.realMin(0), p.getXBase(), 0);
		assertEquals(rpc.realMin(1), p.getYBase(), 0);
		assertEquals(rpc.realMax(0), p.getXBase() + p.getFloatWidth(), 0);
		assertEquals(rpc.realMax(1), p.getYBase() + p.getFloatHeight(), 0);
	}

	@Test
	public void testWrapperToPointRoi() {
		final PointRoi p = convertService.convert(pointRoiWrap, PointRoi.class);
		assertTrue(p == point);
	}

	// -- PointRoi to PointMask --

	@Test
	public void testPointRoiToPointMaskConverterMatching() {
		// PointRoi to PointMask (shouldn't work)
		final PointRoi pr = new PointRoi(11.5, 6);
		final Converter<?, ?> pointRoiToPointMask = convertService.getHandler(pr,
			PointMask.class);
		assertTrue(pointRoiToPointMask == null);

		// Wrapped PointMask to PointMask (should work)
		final WritablePointMask wpm = new DefaultWritablePointMask(new double[] {
			82, 65 });
		final PointRoi wrap = new PointMaskWrapper(wpm);
		final Converter<?, ?> wrapperToPointMask = convertService.getHandler(wrap,
			PointMask.class);
		assertTrue(wrapperToPointMask instanceof WrapperToWritablePointMask);
	}

	@Test
	public void testWrapperToPointMask() {
		final WritablePointMask wpm = new DefaultWritablePointMask(new double[] {
			82, 65 });
		final PointRoi wrap = new PointMaskWrapper(wpm);
		final PointMask converted = convertService.convert(wrap, PointMask.class);
		assertTrue(converted == wpm);
	}

	// -- PointMask To PointRoi tests --

	@Test
	public void testPointMaskToPointRoiConverterMatching() {
		// Read only PointMask to PointRoi (shouldn't wrap)
		final PointMask p = new TestPointMask(new double[] { 11, 0 });
		final Converter<?, ?> c = convertService.getHandler(p, PointRoi.class);
		assertTrue(c instanceof PointMaskToPointRoiConverter);

		// WritablePointMask to PointRoi (should wrap)
		final PointMask pm = new DefaultWritablePointMask(new double[] { 0, 0 });
		final Converter<?, ?> writableToPointRoi = convertService.getHandler(pm,
			PointRoi.class);
		assertTrue(
			writableToPointRoi instanceof WritablePointMaskToPointRoiConverter);
	}

	@Test
	public void testPointMaskToPointRoi() {
		final PointMask pm = new TestPointMask(new double[] { 140.25, -0.5 });
		final PointRoi pr = convertService.convert(pm, PointRoi.class);

		assertFalse(pr instanceof PointMaskWrapper);
		assertEquals(1, pr.getNCoordinates());
		assertEquals(pm.getDoublePosition(0), pr
			.getContainedFloatPoints().xpoints[0], 0);
		assertEquals(pm.getDoublePosition(1), pr
			.getContainedFloatPoints().ypoints[0], 0);
	}

	@Test
	public void testWritablePointMaskToPointRoi() {
		final PointMask pm = new DefaultWritablePointMask(new double[] { 11,
			13.5 });
		final PointRoi pr = convertService.convert(pm, PointRoi.class);

		assertTrue(pr instanceof PointMaskWrapper);
		assertEquals(1, pr.getNCoordinates());
		assertEquals(pm.getDoublePosition(0), pr
			.getContainedFloatPoints().xpoints[0], 0);
		assertEquals(pm.getDoublePosition(1), pr
			.getContainedFloatPoints().ypoints[0], 0);
	}

	// -- Helper classes --

	private static final class TestRPC implements
		RealPointCollection<RealLocalizable>
	{

		@Override
		public boolean test(final RealLocalizable t) {
			return false;
		}

		@Override
		public int numDimensions() {
			return 2;
		}

		@Override
		public double realMin(final int d) {
			return d == 0 ? 0 : -0.25;
		}

		@Override
		public double realMax(final int d) {
			return d == 0 ? 100.5 : 20;
		}

		@Override
		public Iterable<RealLocalizable> points() {
			final List<RealLocalizable> pts = new ArrayList<>(3);
			pts.add(new RealPoint(new double[] { 10, 20 }));
			pts.add(new RealPoint(new double[] { 0, 5 }));
			pts.add(new RealPoint(new double[] { 100.5, -0.25 }));
			return pts;
		}

		@Override
		public long size() {
			return 3;
		}
	}

	private static final class TestPointMask extends AbstractRealLocalizable
		implements PointMask
	{

		public TestPointMask(final double[] position) {
			super(position);
		}

		@Override
		public boolean test(final RealLocalizable t) {
			return false;
		}

	}
}
