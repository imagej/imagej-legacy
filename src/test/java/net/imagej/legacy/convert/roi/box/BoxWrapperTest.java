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
import static org.junit.Assert.assertNotEquals;

import net.imglib2.roi.geom.real.ClosedWritableBox;
import net.imglib2.roi.geom.real.WritableBox;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link BoxWrapper}
 *
 * @author Alison Walter
 */
public class BoxWrapperTest {

	private WritableBox b;
	private BoxWrapper r;

	@Before
	public void setup() {
		b = new ClosedWritableBox(new double[] { 10, 11 }, new double[] { 21, 30 });
		r = new BoxWrapper(b);
	}

	@Test
	public void testGetters() {
		assertEquals(b.center().getDoublePosition(0), r.getXBase() + r
			.getFloatWidth() / 2, 0);
		assertEquals(b.center().getDoublePosition(1), r.getYBase() + r
			.getFloatHeight() / 2, 0);
		assertEquals(b.sideLength(0), r.getFloatWidth(), 0);
		assertEquals(b.sideLength(1), r.getFloatHeight(), 0);
	}

	@Test
	public void testGetSource() {
		assertEquals(b, r.getSource());
	}

	@Test
	public void testSynchronize() {
		r.setLocation(13, 22);

		assertNotEquals(b.center().getDoublePosition(0), r.getXBase() + r
			.getFloatWidth() / 2, 0);
		assertNotEquals(b.center().getDoublePosition(1), r.getYBase() + r
			.getFloatHeight() / 2, 0);

		r.synchronize();

		assertEquals(b.center().getDoublePosition(0), r.getXBase() + r
			.getFloatWidth() / 2, 0);
		assertEquals(b.center().getDoublePosition(1), r.getYBase() + r
			.getFloatHeight() / 2, 0);
		assertEquals(b.sideLength(0), r.getFloatWidth(), 0);
		assertEquals(b.sideLength(1), r.getFloatHeight(), 0);
	}

	@Test
	public void testGetUpdatedSource() {
		r.setLocation(1, 3);

		assertEquals(15.5, b.center().getDoublePosition(0), 0);
		assertEquals(20.5, b.center().getDoublePosition(1), 0);

		final WritableBox wb = r.getUpdatedSource();

		assertEquals(b, wb);
		assertEquals(6.5, b.center().getDoublePosition(0), 0);
		assertEquals(12.5, b.center().getDoublePosition(1), 0);
	}

	@Test
	public void testRoundedCorners() {
		r.setCornerDiameter(12);
		r.synchronize();

		// Setting the corner diameter shouldn't affect the wrapped Box
		assertEquals(b.center().getDoublePosition(0), r.getXBase() + r
			.getFloatWidth() / 2, 0);
		assertEquals(b.center().getDoublePosition(1), r.getYBase() + r
			.getFloatHeight() / 2, 0);
		assertEquals(b.sideLength(0), r.getFloatWidth(), 0);
		assertEquals(b.sideLength(1), r.getFloatHeight(), 0);
	}
}
