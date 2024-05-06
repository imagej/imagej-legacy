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

import ij.IJ;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

import java.lang.reflect.Type;

import static org.junit.Assert.*;

/**
 * Tests if an image title and ID can be converted to Dataset.
 *
 * @author Mattias Arzt
 */
public class StringToDatasetConverterTest {

	static {
		LegacyInjector.preinit();
	}

	private final static String NON_IMAGE_TITLE = "no image";
	private final static String IMAGE_TITLE = "image title";
	private final static int PIXEL_VALUE = 45;

	private Context context;
	private ConvertService convertService;
	private Converter<String, Dataset> converter;
	private int imageId;

	@Before
	public void setUp() {
		context = new Context();
		convertService = context.service(ConvertService.class);
		ImagePlus imp = IJ.createImage(IMAGE_TITLE, 10, 10, 1, 8);
		imp.getProcessor().set(0, 0, PIXEL_VALUE);
		imp.show();
		imageId = imp.getID();
		converter = convertService.getInstance(StringToDatasetConverter.class);
	}

	@After
	public void teardown() {
		context.dispose();
	}

	/**
	 * Tests {@link StringToDatasetConverter#convert(Object, Class)}
	 */
	@Test
	public void testConvertTitle() {
		Dataset dataset = convertService.convert(IMAGE_TITLE, Dataset.class);
		assertEquals(PIXEL_VALUE, dataset.getAt(0, 0).getRealDouble(), 0.0);
	}

	@Test
	public void testConvertId() {
		Dataset dataset =
			convertService.convert(String.valueOf(imageId), Dataset.class);
		assertEquals(PIXEL_VALUE, dataset.getAt(0, 0).getRealDouble(), 0.0);
	}

	/**
	 * Tests if the converter behaves as expected when the string does not
	 * match any image title.
	 */
	@Test
	public void testNonImageTitle()
	{
		assertFalse(convertService.supports(NON_IMAGE_TITLE, Dataset.class));
		assertNull(convertService.convert(NON_IMAGE_TITLE, Dataset.class));
	}

	/**
	 * Test {@link StringToDatasetConverter#canConvert(Object, Class)}.
	 */
	@Test
	public void testCanConvertTitle() {
		assertTrue(converter.canConvert(IMAGE_TITLE, Dataset.class));
		assertTrue(converter.canConvert(IMAGE_TITLE, Img.class));
		assertTrue(
			converter.canConvert(IMAGE_TITLE, RandomAccessibleInterval.class));
		assertFalse(converter.canConvert(IMAGE_TITLE, ImagePlus.class));
		assertFalse(converter.canConvert(NON_IMAGE_TITLE, Dataset.class));
	}

	/**
	 * Test {@link StringToDatasetConverter#canConvert(Object, Class)}.
	 */
	@Test
	public void testCanConvertId() {
		assertTrue(
			converter.canConvert(String.valueOf(imageId), Dataset.class));

		// NB: Should return false if the given number is not an image ID.
		assertFalse(converter.canConvert("392847", Dataset.class));
	}

	/**
	 * Test {@link StringToDatasetConverter#canConvert(Object, Type)}.
	 */
	@Test
	public void testCanConvert2() {
		assertTrue(converter.canConvert(IMAGE_TITLE, (Type) Dataset.class));
		assertFalse(converter.canConvert(IMAGE_TITLE, (Type) ImagePlus.class));
		assertFalse(converter.canConvert(NON_IMAGE_TITLE, (Type) Dataset.class));
	}
}
