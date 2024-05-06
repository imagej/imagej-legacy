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

package net.imagej.legacy.convert.roi.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.gui.Roi;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.Box;
import net.imglib2.roi.geom.real.ClosedWritableBox;
import net.imglib2.roi.geom.real.WritableBox;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests {@link RoiWrapper}.
 *
 * @author Alison Walter
 */
public class RoiWrapperTest {

	private Roi rect;
	private WritableBox wrap;
	private Box b;
	private RealLocalizable inside;
	private RealLocalizable outside;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		rect = new Roi(1, 13, 7, 4);
		wrap = new RoiWrapper(rect);
		b = new ClosedWritableBox(new double[] { 1, 13 }, new double[] { 8, 17 });
		inside = new RealPoint(new double[] { 6.25, 15 });
		outside = new RealPoint(new double[] { -2, 12.5 });
	}

	@Test
	public void testRoiWrapperGetters() {
		assertEquals(rect.getXBase() + rect.getFloatWidth() / 2, wrap.center()
			.getDoublePosition(0), 0);
		assertEquals(rect.getYBase() + rect.getFloatHeight() / 2, wrap.center()
			.getDoublePosition(1), 0);
		assertEquals(rect.getFloatWidth(), wrap.sideLength(0), 0);
		assertEquals(rect.getFloatHeight(), wrap.sideLength(1), 0);

		assertEquals(b.center().getDoublePosition(0), wrap.center()
			.getDoublePosition(0), 0);
		assertEquals(b.center().getDoublePosition(1), wrap.center()
			.getDoublePosition(1), 0);
		assertEquals(b.sideLength(0), wrap.sideLength(0), 0);
		assertEquals(b.sideLength(1), wrap.sideLength(1), 0);
	}

	@Test
	public void testRoiWrapperSetCenter() {
		wrap.center().setPosition(new double[] { 1, 11.5 });

		assertEquals(1, wrap.center().getDoublePosition(0), 0);
		assertEquals(11.5, wrap.center().getDoublePosition(1), 0);

		// check if backing Roi was updated
		assertEquals(-2.5, rect.getXBase(), 0);
		assertEquals(9.5, rect.getYBase(), 0);
		assertEquals(7, rect.getFloatWidth(), 0);
		assertEquals(4, rect.getFloatHeight(), 0);
	}

	@Test
	public void testRoiWrapperSetSideLength() {
		exception.expect(UnsupportedOperationException.class);
		wrap.setSideLength(0, 3);
	}

	@Test
	public void testRoiWrapperTest() {
		// Test corners
		final RealLocalizable topLeft = new RealPoint(new double[] { 1, 13 });
		final RealLocalizable bottomLeft = new RealPoint(new double[] { 1, 17 });
		final RealLocalizable topRight = new RealPoint(new double[] { 8, 13 });
		final RealLocalizable bottomRight = new RealPoint(new double[] { 8, 17 });

		assertTrue(wrap.test(topLeft));
		assertFalse(wrap.test(bottomLeft));
		assertFalse(wrap.test(bottomRight));
		assertFalse(wrap.test(topRight));

		// Test edges
		final RealLocalizable top = new RealPoint(new double[] { 3.25, 13 });
		final RealLocalizable left = new RealPoint(new double[] { 1, 15 });
		final RealLocalizable right = new RealPoint(new double[] { 8, 14.125 });
		final RealLocalizable bottom = new RealPoint(new double[] { 5.5, 17 });

		assertTrue(wrap.test(top));
		assertTrue(wrap.test(left));
		assertFalse(wrap.test(bottom));
		assertFalse(wrap.test(right));

		// Test points
		assertTrue(wrap.test(inside));
		assertFalse(wrap.test(outside));
	}

	@Test
	public void testRoiWrapperBounds() {
		assertEquals(1, wrap.realMin(0), 0);
		assertEquals(13, wrap.realMin(1), 0);
		assertEquals(8, wrap.realMax(0), 0);
		assertEquals(17, wrap.realMax(1), 0);
	}

	@Test
	public void testRoiWrapperAfterMoved() {
		wrap.center().setPosition(new double[] { 1, 11.5 });

		// Check test
		assertFalse(wrap.test(inside));
		assertTrue(wrap.test(outside));

		// Check getters
		assertEquals(1, wrap.center().getDoublePosition(0), 0);
		assertEquals(11.5, wrap.center().getDoublePosition(1), 0);
		assertEquals(7, wrap.sideLength(0), 0);
		assertEquals(4, wrap.sideLength(1), 0);

		// Check bounds
		assertEquals(-2.5, wrap.realMin(0), 0);
		assertEquals(9.5, wrap.realMin(1), 0);
		assertEquals(4.5, wrap.realMax(0), 0);
		assertEquals(13.5, wrap.realMax(1), 0);
	}

}
