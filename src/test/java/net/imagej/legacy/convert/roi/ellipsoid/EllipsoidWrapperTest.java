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
import static org.junit.Assert.assertNotEquals;

import net.imglib2.roi.geom.real.OpenWritableEllipsoid;
import net.imglib2.roi.geom.real.WritableEllipsoid;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link EllipsoidWrapper}
 *
 * @author Alison Walter
 */
public class EllipsoidWrapperTest {

	private WritableEllipsoid e;
	private EllipsoidWrapper o;

	@Before
	public void setup() {
		e = new OpenWritableEllipsoid(new double[] { 15, 6.5 }, new double[] { 10,
			3 });
		o = new EllipsoidWrapper(e);
	}

	@Test
	public void testGetters() {
		assertEquals(e.center().getDoublePosition(0), o.getXBase() + o
			.getFloatWidth() / 2, 0);
		assertEquals(e.center().getDoublePosition(1), o.getYBase() + o
			.getFloatHeight() / 2, 0);
		assertEquals(e.semiAxisLength(0), o.getFloatWidth() / 2, 0);
		assertEquals(e.semiAxisLength(1), o.getFloatHeight() / 2, 0);
	}

	@Test
	public void testGetSource() {
		assertEquals(e, o.getSource());
	}

	@Test
	public void testSynchronize() {
		o.setLocation(22, 105.5);

		assertNotEquals(e.center().getDoublePosition(0), o.getXBase() + o
			.getFloatWidth() / 2, 0);
		assertNotEquals(e.center().getDoublePosition(1), o.getYBase() + o
			.getFloatHeight() / 2, 0);

		o.synchronize();
		assertEquals(e.center().getDoublePosition(0), o.getXBase() + o
			.getFloatWidth() / 2, 0);
		assertEquals(e.center().getDoublePosition(1), o.getYBase() + o
			.getFloatHeight() / 2, 0);
		assertEquals(e.semiAxisLength(0), o.getFloatWidth() / 2, 0);
		assertEquals(e.semiAxisLength(1), o.getFloatHeight() / 2, 0);
	}

	@Test
	public void testGetUpdatedSource() {
		o.setLocation(0, 0);

		assertEquals(15, e.center().getDoublePosition(0), 0);
		assertEquals(6.5, e.center().getDoublePosition(1), 0);

		final WritableEllipsoid we = o.getUpdatedSource();

		assertEquals(e, we);
		assertEquals(10, e.center().getDoublePosition(0), 0);
		assertEquals(3, e.center().getDoublePosition(1), 0);
	}

}
