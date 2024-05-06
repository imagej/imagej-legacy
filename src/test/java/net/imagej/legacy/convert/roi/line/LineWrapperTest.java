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
import static org.junit.Assert.assertNotEquals;

import net.imglib2.roi.geom.real.DefaultWritableLine;
import net.imglib2.roi.geom.real.WritableLine;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link LineWrapper}
 *
 * @author Alison Walter
 */
public class LineWrapperTest {

	private WritableLine l;
	private LineWrapper w;

	@Before
	public void setup() {
		l = new DefaultWritableLine(new double[] { 12, 14.5 }, new double[] { 23.25,
			67 }, false);
		w = new LineWrapper(l);
	}

	@Test
	public void testGetters() {
		assertEquals(l.endpointOne().getDoublePosition(0), w.x1d, 0);
		assertEquals(l.endpointOne().getDoublePosition(1), w.y1d, 0);
		assertEquals(l.endpointTwo().getDoublePosition(0), w.x2d, 0);
		assertEquals(l.endpointTwo().getDoublePosition(1), w.y2d, 0);
	}

	@Test
	public void testGetSource() {
		assertEquals(l, w.getSource());
	}

	@Test
	public void testSynchronize() {
		w.x1d = 3;
		w.y1d = 55.25;
		w.x2d = 100;
		w.y2d = 10;

		assertNotEquals(l.endpointOne().getDoublePosition(0), w.x1d, 0);
		assertNotEquals(l.endpointOne().getDoublePosition(1), w.y1d, 0);
		assertNotEquals(l.endpointTwo().getDoublePosition(0), w.x2d, 0);
		assertNotEquals(l.endpointTwo().getDoublePosition(1), w.y2d, 0);

		w.synchronize();

		assertEquals(l.endpointOne().getDoublePosition(0), w.x1d, 0);
		assertEquals(l.endpointOne().getDoublePosition(1), w.y1d, 0);
		assertEquals(l.endpointTwo().getDoublePosition(0), w.x2d, 0);
		assertEquals(l.endpointTwo().getDoublePosition(1), w.y2d, 0);

	}

	@Test
	public void testGetUpdatedSource() {
		w.x1d = 0;

		assertEquals(12, l.endpointOne().getDoublePosition(0), 0);
		assertEquals(14.5, l.endpointOne().getDoublePosition(1), 0);
		assertEquals(23.25, l.endpointTwo().getDoublePosition(0), 0);
		assertEquals(67, l.endpointTwo().getDoublePosition(1), 0);

		final WritableLine wl = w.getUpdatedSource();

		assertEquals(l, wl);
		assertEquals(0, l.endpointOne().getDoublePosition(0), 0);
		assertEquals(14.5, l.endpointOne().getDoublePosition(1), 0);
		assertEquals(23.25, l.endpointTwo().getDoublePosition(0), 0);
		assertEquals(67, l.endpointTwo().getDoublePosition(1), 0);
	}

	@Test
	public void testLineWithWidth() {
		ij.gui.Line.setWidth(2);
		w.synchronize();

		// Setting the width shouldn't change the ImgLib2 Line
		assertEquals(12, l.endpointOne().getDoublePosition(0), 0);
		assertEquals(14.5, l.endpointOne().getDoublePosition(1), 0);
		assertEquals(23.25, l.endpointTwo().getDoublePosition(0), 0);
		assertEquals(67, l.endpointTwo().getDoublePosition(1), 0);
	}
}
