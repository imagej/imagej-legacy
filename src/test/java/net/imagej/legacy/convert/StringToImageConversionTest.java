package net.imagej.legacy.convert;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

import ij.ImagePlus;
import ij.WindowManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import net.imagej.Dataset;

public class StringToImageConversionTest {

	private Context context;
	private ConvertService convertService;

	private String title1 = "Some other title";
	private String title2 = "Image 2";
	private String nonExistentTitle = "Non-existent Image";
	private String nonExistentIDString = "-123456789";

	private int id1;
	private int id2;
	private int width1 = 100;
	private int height1 = 200;
	private int width2 = 20;
	private int height2 = 10;

	@Before
	public void setUp() {
		context = new Context();
		convertService = context.service(ConvertService.class);

		ImageProcessor ip1 = new FloatProcessor(width1, height1);
		ImagePlus imp1 = new ImagePlus(title1, ip1);
		imp1.show();
		id1 = imp1.getID();

		ImageProcessor ip2 = new FloatProcessor(width2, height2);
		ImagePlus imp2 = new ImagePlus(title2, ip2);
		imp2.show();
		id2 = imp2.getID();
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	/**
	 * Assert that "Image 2" is the current image.
	 */
	@Test
	public void testCurrentImage() {
		assertEquals(title2, WindowManager.getCurrentImage().getTitle());		
		assertEquals(id2, WindowManager.getCurrentImage().getID());
	}

	/**
	 * Assert that {@link StringToImagePlusConverter} doesn't match a non-existent
	 * title or ID.
	 */
	@Test
	public void testNonExistentImagePlus() {
		// assertFalse(convertService.supports(nonExistentTitle, ImagePlus.class)); // fails
		Converter<?, ?> converterByTitle = convertService.getHandler(nonExistentTitle, ImagePlus.class);
		// NB: this returns org.scijava.convert.DefaultConverter
		// We assert that StringToImagePlusConverter does *not* match.
		assertNotEquals(StringToImagePlusConverter.class, converterByTitle.getClass());
		
		// assertFalse(convertService.supports(nonExistentTitle, ImagePlus.class)); // fails
		Converter<?, ?> converterByID = convertService.getHandler(nonExistentIDString, ImagePlus.class);
		// NB: this returns org.scijava.convert.DefaultConverter
		// We assert that StringToImagePlusConverter does *not* match.
		assertNotEquals(StringToImagePlusConverter.class, converterByID.getClass());
	}

	/**
	 * Assert we can get an opened image by its title.
	 */
	@Test
	public void testOpenedImagePlusFromTitle() {
		assertTrue(convertService.supports(title1, ImagePlus.class));
		
		Converter<?, ?> converter = convertService.getHandler(title1, ImagePlus.class);
		assertEquals(StringToImagePlusConverter.class, converter.getClass());

		ImagePlus converted = convertService.convert(title1, ImagePlus.class);
		assertEquals(title1, converted.getTitle());
		assertEquals(id1, converted.getID());

		assertEquals(width1, converted.getWidth());
		assertEquals(height1, converted.getHeight());
	}

	/**
	 * Assert we can get an opened image by its ID.
	 */
	@Test
	public void testOpenedImagePlusFromID() {
		assertTrue(convertService.supports("" + id1, ImagePlus.class));
		Converter<?, ?> converter = convertService.getHandler("" + id1, ImagePlus.class);
		assertEquals(StringToImagePlusConverter.class, converter.getClass());

		ImagePlus converted = convertService.convert("" + id1, ImagePlus.class);
		assertEquals(title1, converted.getTitle());
		assertEquals(id1, converted.getID());

		assertEquals(width1, converted.getWidth());
		assertEquals(height1, converted.getHeight());
	}

	/**
	 * Assert we can get an opened image by its title.
	 */
	@Test
	public void testOpenedDatasetFromTitle() {
		assertTrue(convertService.supports(title1, Dataset.class));
		
		Dataset converted = convertService.convert(title1, Dataset.class);
		assertEquals(title1, converted.getName());

		assertEquals(width1, converted.getWidth());
		assertEquals(height1, converted.getHeight());
	}

	/**
	 * Assert we can get an opened image by its ID.
	 */
	@Test
	public void testOpenedDatasetFromID() {
		assertTrue(convertService.supports("" + id1, Dataset.class));

		Dataset converted = convertService.convert("" + id1, Dataset.class);
		assertEquals(title1, converted.getName());

		assertEquals(width1, converted.getWidth());
		assertEquals(height1, converted.getHeight());
	}
}
