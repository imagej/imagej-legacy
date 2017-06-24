/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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

package net.imagej.legacy.translate;

import static org.junit.Assert.assertEquals;

import ij.ImagePlus;

import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

/**
 * Tests {@link GrayImagePlusCreator}.
 *
 * @author Curtis Rueden
 */
public class GrayImagePlusCreatorTest {

	static {
		LegacyInjector.preinit();
	}

	private Context context;

	@Before
	public void setUp() {
		context = new Context();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	/** Tests {@link GrayImagePlusCreator#createLegacyImage(Dataset)}. */
	@Test
	public void testCreateLegacyImage() {
		final GrayImagePlusCreator grayImagePlusCreator = //
			new GrayImagePlusCreator(context);
		final int w = 100, h = 101, c = 2, t = 3, z = 5;
		final Dataset ds = makeDataset(w, h, c, t, z);
		ImagePlus imp = grayImagePlusCreator.createLegacyImage(ds);
		assertDimensionsMatch(ds, imp);
	}

	private void assertDimensionsMatch(final Dataset ds, final Object o) {
		ImagePlus imp = (ImagePlus) o; // NB: Avoid IJ1 class leakage.
		assertEquals(ds.dimension(0), imp.getWidth());
		assertEquals(ds.dimension(1), imp.getHeight());
		assertEquals(ds.dimension(2), imp.getNChannels());
		assertEquals(ds.dimension(3), imp.getNSlices());
		assertEquals(ds.dimension(4), imp.getNFrames());
	}

	private Dataset makeDataset(final long... dims) {
		final Img<UnsignedByteType> img = ArrayImgs.unsignedBytes(dims);
		final ImgPlus<UnsignedByteType> imgPlus = new ImgPlus<>(img);
		return new DefaultDataset(context, imgPlus);
	}
}
