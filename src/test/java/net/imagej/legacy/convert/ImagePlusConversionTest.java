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

package net.imagej.legacy.convert;

import static org.junit.Assert.assertEquals;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.display.ImageDisplay;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.display.DisplayService;

/**
 * Tests the converters of the {@link net.imagej.legacy.convert} package.
 * 
 * @author Curtis Rueden
 */
public class ImagePlusConversionTest {

	static {
			LegacyInjector.preinit();
	}

	private Context context;
	private ConvertService convertService;
	private DatasetService datasetService;
	private DisplayService displayService;

	@Before
	public void setUp() {
		context = new Context();
		convertService = context.service(ConvertService.class);
		datasetService = context.service(DatasetService.class);
		displayService = context.service(DisplayService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	/** Tests {@link DatasetToImagePlusConverter}. */
	@Test
	public void testDatasetToImagePlus() {
		final Dataset d = createDataset();
		final ImagePlus imp = convertService.convert(d, ImagePlus.class);
		assertImagesEqual(imp, d);
	}

	/** Tests {@link ImageDisplayToImagePlusConverter}. */
	@Test
	public void testImageDisplayToImagePlus() {
		final ImageDisplay ds = createImageDisplay();
		final ImagePlus imp = convertService.convert(ds, ImagePlus.class);
		assertImagesEqual(imp, ds);
	}

	/** Tests {@link ImagePlusToDatasetConverter}. */
	@Test
	public void testImagePlusToDataset() {
		final Object imp = createImagePlus();
		final Dataset d = convertService.convert(imp, Dataset.class);
		assertImagesEqual(imp, d);
	}

	/** Tests {@link ImagePlusToImageDisplayConverter}. */
	@Test
	public void testImagePlusToImageDisplay() {
		final Object imp = createImagePlus();
		final ImageDisplay ds = convertService.convert(imp, ImageDisplay.class);
		assertImagesEqual(imp, ds);
	}

	// -- Helper methods --

	private void assertImagesEqual(final Object o, final ImageDisplay ds) {
		assertEquals(1, ds.size());
		assertImagesEqual(o, (Dataset) ds.get(0).getData());
	}

	private void assertImagesEqual(final Object o, final Dataset d) {
		final ImagePlus imp = (ImagePlus) o;
		assertEquals(imp.getTitle(), d.getName());
		assertEquals(imp.getWidth(), d.dimension(0));
		assertEquals(imp.getHeight(), d.dimension(1));
		assertEquals(imp.getNSlices(), d.dimension(2));
		final RandomAccess<RealType<?>> ra = d.randomAccess();
		for (int z=0; z<imp.getNSlices(); z++) {
			ra.setPosition(z, 2);
			final ImageProcessor ip = imp.getStack().getProcessor(z + 1);
			for (int y=0; y<imp.getHeight(); y++) {
				ra.setPosition(y, 1);
				for (int x=0; x<imp.getWidth(); x++) {
					ra.setPosition(x, 0);
					assertEquals(ip.get(x, y), ra.get().getRealDouble(), 0);
				}
			}
		}
	}

	private Dataset createDataset() {
		final int width = 64, height = 64, depth = 5;

		// create an empty dataset
		final String name = "Gradient Image";
		final long[] dims = { width, height, depth };
		final AxisType[] axes = { Axes.X, Axes.Y, Axes.Z };
		final Dataset dataset = datasetService.create(new UnsignedByteType(), //
			dims, name, axes);

		for (int z = 0; z < depth; z++) {
			// generate a byte array containing the diagonal gradient
			final byte[] data = new byte[width * height];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					final int index = y * width + x;
					data[index] = (byte) (x + y + z);
				}
			}

			// populate the dataset with the gradient data
			dataset.setPlane(z, data);
		}

		return dataset;
	}

	private ImageDisplay createImageDisplay() {
		final ImageDisplay ds = (ImageDisplay) //
			displayService.createDisplay(createDataset());
		return ds;
	}

	private Object createImagePlus() {
		final int width = 64, height = 64, depth = 5;
		return IJ.createImage("gradient", "8-bit ramp", width, height, depth);
	}
}
