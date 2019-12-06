
package net.imagej.legacy.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.scijava.Context;
import org.scijava.convert.ConvertService;

import ij.ImagePlus;

@RunWith(Parameterized.class)
public class ImgPlusToImagePlusConversionTest<T extends NativeType<T>> {

	private Context context;
	private ConvertService convertService;

	private ImgPlus<T> testImgPlus;

	@Parameters (name = "{0}")
	public static Object[] params() {
		return new Object[] { //
			new Object[] { //
				"UnsignedByteType XYZ",
				UnsignedByteType.class,
				new AxisType[] { Axes.X, Axes.Y, Axes.Z }
			},
			new Object[] { //
				"UnsignedByteType XYC",
				UnsignedByteType.class,
				new AxisType[] { Axes.X, Axes.Y, Axes.CHANNEL }
			}
		};
	}

	public ImgPlusToImagePlusConversionTest(String name, Class<T> type, AxisType[] axes) {
		try {
			ArrayImg<T, ?> testImg = new ArrayImgFactory<>(type.newInstance()).create(new int[] {16, 16});
			testImgPlus = new ImgPlus<>(testImg, name, axes, new double[] {0.1, 0.2, 0.3}, new String[] {"um", "nm", "cm"});
		}
		catch (InstantiationException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
		catch (IllegalAccessException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
	}

	@Before
	public void setUp() {
		context = new Context(ConvertService.class);
		convertService = context.getService(ConvertService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testImgPlusToImagePlus() {
		assertTrue(convertService.supports(testImgPlus, ImagePlus.class));

		ImagePlus imp = convertService.convert(testImgPlus, ImagePlus.class);
		assertImagesEqual(imp, testImgPlus);
	}

	private void assertImagesEqual(ImagePlus imp, ImgPlus<?> img) {
		assertEquals(imp.getTitle(), img.getName());
		fail("Not yet implemented");
	}
}
