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

package net.imagej.legacy.convert.roi.ellipsoid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.gui.OvalRoi;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.ClosedWritableEllipsoid;
import net.imglib2.roi.geom.real.Ellipsoid;
import net.imglib2.roi.geom.real.WritableEllipsoid;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests {@link OvalRoiWrapper}
 *
 * @author Alison Walter
 */
public class OvalRoiWrapperTest {

	private OvalRoi oval;
	private Ellipsoid e;
	private WritableEllipsoid wrap;
	private RealLocalizable inside;
	private RealLocalizable onBoundary;
	private RealLocalizable outside;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		oval = new OvalRoi(10, 22, 7, 4);
		e = new ClosedWritableEllipsoid(new double[] { 13.5, 24 }, new double[] {
			3.5, 2 });
		wrap = new OvalRoiWrapper(oval);

		inside = new RealPoint(new double[] { 12.125, 23 });
		onBoundary = new RealPoint(new double[] { 17, 24 });
		outside = new RealPoint(new double[] { 101, 41.125 });
	}

	// -- OvalWrapper tests --

	@Test
	public void testOvalWrapperGetters() {
		// Test ImageJ 1.x and wrapper equivalent
		assertEquals(oval.getFloatWidth() / 2, wrap.semiAxisLength(0), 0);
		assertEquals(oval.getFloatHeight() / 2, wrap.semiAxisLength(1), 0);
		assertEquals(oval.getXBase() + oval.getFloatWidth() / 2, wrap.center()
			.getDoublePosition(0), 0);
		assertEquals(oval.getYBase() + oval.getFloatHeight() / 2, wrap.center()
			.getDoublePosition(1), 0);

		// Test ImgLib2 and wrapper equivalent
		assertEquals(e.semiAxisLength(0), wrap.semiAxisLength(0), 0);
		assertEquals(e.semiAxisLength(1), wrap.semiAxisLength(1), 0);
		assertEquals(e.center().getDoublePosition(0), wrap.center()
			.getDoublePosition(0), 0);
		assertEquals(e.center().getDoublePosition(1), wrap.center()
			.getDoublePosition(1), 0);
	}

	@Test
	public void testOvalWrapperSetCenter() {
		wrap.center().setPosition(new double[] { 100, 40.5 });
		assertEquals(100, wrap.center().getDoublePosition(0), 0);
		assertEquals(40.5, wrap.center().getDoublePosition(1), 0);

		// check that underlying OvalRoi was updated
		assertEquals(oval.getXBase() + oval.getFloatWidth() / 2, wrap.center()
			.getDoublePosition(0), 0);
		assertEquals(oval.getYBase() + oval.getFloatHeight() / 2, wrap.center()
			.getDoublePosition(1), 0);
	}

	@Test
	public void testOvalWrapperSetSemiAxisLength() {
		exception.expect(UnsupportedOperationException.class);
		wrap.setSemiAxisLength(0, 3);
	}

	@Test
	public void testOvalWrapperTest() {
		assertEquals(e.test(inside), wrap.test(inside));
		assertEquals(e.test(onBoundary), wrap.test(onBoundary));
		assertEquals(e.test(outside), wrap.test(outside));
	}

	@Test
	public void testOvalWrapperBounds() {
		assertEquals(10, wrap.realMin(0), 0);
		assertEquals(22, wrap.realMin(1), 0);
		assertEquals(17, wrap.realMax(0), 0);
		assertEquals(26, wrap.realMax(1), 0);
	}

	@Test
	public void testOvalWrapperAfterMoved() {
		final RealLocalizable onNewBoundary = new RealPoint(new double[] { 100,
			42.5 });

		assertTrue(wrap.test(inside));
		assertTrue(wrap.test(onBoundary));
		assertFalse(wrap.test(outside));
		assertFalse(wrap.test(onNewBoundary));

		wrap.center().setPosition(new double[] { 100, 40.5 });

		// Check that move occurred
		assertFalse(wrap.test(inside));
		assertFalse(wrap.test(onBoundary));
		assertTrue(wrap.test(outside));
		assertTrue(wrap.test(onNewBoundary));

		// Check that the bounds are updated
		assertEquals(96.5, wrap.realMin(0), 0);
		assertEquals(38.5, wrap.realMin(1), 0);
		assertEquals(103.5, wrap.realMax(0), 0);
		assertEquals(42.5, wrap.realMax(1), 0);

		// Check that underlying OvalRoi was updated
		assertEquals(oval.getFloatWidth() / 2, wrap.semiAxisLength(0), 0);
		assertEquals(oval.getFloatHeight() / 2, wrap.semiAxisLength(1), 0);
		assertEquals(oval.getXBase() + oval.getFloatWidth() / 2, wrap.center()
			.getDoublePosition(0), 0);
		assertEquals(oval.getYBase() + oval.getFloatHeight() / 2, wrap.center()
			.getDoublePosition(1), 0);
	}

}
