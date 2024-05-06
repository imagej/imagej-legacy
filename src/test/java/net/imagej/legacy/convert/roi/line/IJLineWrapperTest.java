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

package net.imagej.legacy.convert.roi.line;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geom.real.DefaultWritableLine;
import net.imglib2.roi.geom.real.Line;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests {@link IJLineWrapper}
 *
 * @author Alison Walter
 */
public class IJLineWrapperTest {

	private static ij.gui.Line ijLine;
	private static Line ilLine;
	private static Line wrap;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@BeforeClass
	public static void initialize() {
		ijLine = new ij.gui.Line(10.5, 20, 120.5, 150);
		ilLine = new DefaultWritableLine(new double[] { 10.5, 20 }, new double[] {
			120.5, 150 }, false);
		// NB: Width is a property of all lines and may have been set in a separate
		// test class
		ij.gui.Line.setWidth(1);
		wrap = new IJLineWrapper(ijLine);
	}

	@Before
	public void setup() {
		ij.gui.Line.setWidth(1);
	}

	// -- LineWrapper tests --

	@Test
	public void testLineWrapperGetter() {
		// Wrapped Line equals ImageJ 1.x Line
		assertEquals(wrap.endpointOne().getDoublePosition(0), ijLine.x1d, 0);
		assertEquals(wrap.endpointOne().getDoublePosition(1), ijLine.y1d, 0);
		assertEquals(wrap.endpointTwo().getDoublePosition(0), ijLine.x2d, 0);
		assertEquals(wrap.endpointTwo().getDoublePosition(1), ijLine.y2d, 0);

		// Wrapped Line equals equivalent ImgLib2 Line
		assertTrue(equalsRealLocalizable(wrap.endpointOne(), ilLine.endpointOne()));
		assertTrue(equalsRealLocalizable(wrap.endpointTwo(), ilLine.endpointTwo()));
	}

	@Test
	public void testLineWrapperTest() {
		// Test that wrapped line and Imglib2 line have same test behavior
		final RealPoint pt1 = new RealPoint(ilLine.endpointOne());
		final RealPoint pt2 = new RealPoint(ilLine.endpointTwo());
		final RealPoint pt3 = new RealPoint(new double[] { 17, 126 }); // on
		// line
		// on line but beyond endpoint
		final RealPoint pt4 = new RealPoint(new double[] { 4, 115 });
		// off line
		final RealPoint pt5 = new RealPoint(new double[] { 20.25, 40.125 });

		assertEquals(ilLine.test(pt1), wrap.test(pt1));
		assertEquals(ilLine.test(pt2), wrap.test(pt2));
		assertEquals(ilLine.test(pt3), wrap.test(pt3));
		assertEquals(ilLine.test(pt4), wrap.test(pt4));
		assertEquals(ilLine.test(pt5), wrap.test(pt5));
	}

	@Test
	public void testLineWrapperBounds() {
		assertEquals(10.5, wrap.realMin(0), 0);
		assertEquals(20, wrap.realMin(1), 0);
		assertEquals(120.5, wrap.realMax(0), 0);
		assertEquals(150, wrap.realMax(1), 0);
	}

	// -- Helper methods --

	private boolean equalsRealLocalizable(final RealLocalizable pointOne,
		final RealLocalizable pointTwo)
	{
		if (pointOne.numDimensions() != pointTwo.numDimensions()) return false;
		for (int d = 0; d < pointOne.numDimensions(); d++) {
			if (pointOne.getDoublePosition(d) != pointTwo.getDoublePosition(d))
				return false;
		}
		return true;
	}
}
