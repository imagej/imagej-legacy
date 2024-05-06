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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.IJ;
import ij.ImagePlus;

import java.lang.reflect.Type;

import net.imagej.Dataset;
import net.imagej.patcher.LegacyInjector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests if an image title and ID can be converted to ImagePlus.
 *
 * @author Mattias Arzt
 */
public class StringToImagePlusConverterTest {

	static {
		LegacyInjector.preinit();
	}

	private final static String IMAGE_TITLE = "image title";
	private final static String NON_IMAGE_TITLE = "no image";

	private Context context;
	private ConvertService convertService;
	private Converter<String, ImagePlus> converter;
	private Object image;
	private int imageId;

	@Before
	public void setUp() {
		context = new Context();
		convertService = context.service(ConvertService.class);
		ImagePlus imp = IJ.createImage(IMAGE_TITLE, 10, 10, 1, 8);
		imp.show();
		image = imp;
		imageId = imp.getID();
		converter = convertService.getInstance(StringToImagePlusConverter.class);
	}

	@After
	public void teardown() {
		context.dispose();
	}

	/**
	 * Tests {@link StringToImagePlusConverter#convert(Object, Class)}
	 */
	@Test
	public void testConvertTitle() {
		ImagePlus converted = convertService.convert(IMAGE_TITLE, ImagePlus.class);
		assertEquals(image, converted);
	}

	@Test
	public void testConvertId() {
		ImagePlus converted =
			convertService.convert(String.valueOf(imageId), ImagePlus.class);
		assertEquals(image, converted);
	}

	/**
	 * Tests if the converter behaves as expected when the string does not
	 * match any image title.
	 */
	@Test
	public void testNonImageTitle()
	{
		assertFalse(convertService.getHandler(NON_IMAGE_TITLE, ImagePlus.class)
			instanceof StringToImagePlusConverter);
	}

	/**
	 * Test {@link StringToImagePlusConverter#canConvert(Object, Class)}.
	 */
	@Test
	public void testCanConvertTitle() {
		assertTrue(converter.canConvert(IMAGE_TITLE, ImagePlus.class));
		assertFalse(converter.canConvert(IMAGE_TITLE, Dataset.class));
		assertFalse(converter.canConvert(NON_IMAGE_TITLE, ImagePlus.class));
	}

	/**
	 * Test {@link StringToImagePlusConverter#canConvert(Object, Class)}.
	 */
	@Test
	public void testCanConvertId() {
		assertTrue(
			converter.canConvert(String.valueOf(imageId), ImagePlus.class));

		// NB: Should return false if the given number is not an image ID.
		assertFalse(converter.canConvert("392847", ImagePlus.class));
	}

	/**
	 * Test {@link StringToImagePlusConverter#canConvert(Object, Type)}.
	 */
	@Test
	public void testCanConvert2() {
		assertTrue(converter.canConvert(IMAGE_TITLE, (Type) ImagePlus.class));
		assertFalse(converter.canConvert(IMAGE_TITLE, (Type) Dataset.class));
		assertFalse(converter.canConvert(NON_IMAGE_TITLE, (Type) ImagePlus.class));
	}
}
