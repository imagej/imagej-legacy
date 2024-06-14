package net.imagej.legacy.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.convert.ConvertService;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

public class FromImagePlusConversionTest {

	static {
		LegacyInjector.preinit();
	}

	private Context context;
	private ConvertService convertService;

	@Before
	public void setUp() {
		context = new Context();
		convertService = context.getService(ConvertService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	// --- ImagePlus => * ---

	@Test
	public void testImagePlusToDataset() {
		Object imp = createImagePlus();
		Dataset dataset = convertService.convert(imp, Dataset.class);
		assertNotNull(dataset);
		assertImagesEqual(imp, dataset);
		assertCalibrationEqual(imp, dataset);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testImagePlusToImgPlus() {
		Object imp = createImagePlus();
		ImgPlus<RealType<?>> imgPlus = convertService.convert(imp, ImgPlus.class);
		assertNotNull(imgPlus);
		assertImagesEqual(imp, imgPlus);
		assertCalibrationEqual(imp, imgPlus);
	}

	@Test
	public void testImagePlusToImageDisplay() {
		Object imp = createImagePlus();
		ImageDisplay imageDisplay = convertService.convert(imp, ImageDisplay.class);
		assertNotNull(imageDisplay);
		assertImagesEqual(imp, imageDisplay);
		assertCalibrationEqual(imp, imageDisplay);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testImagePlusToImg() {
		Object imp = createImagePlus();
		Img<RealType<?>> img = convertService.convert(imp, Img.class);
		assertNotNull(img);
		assertImagesEqual(imp, img);
	}

	// --- Synchronization of current plane position ---

	@Test
	public void testImageDisplayToImagePlusSynchronization() {
		ImagePlus imp = (ImagePlus) createImagePlus();
		imp.show();
		ImageDisplay imageDisplay = convertService.convert(imp, ImageDisplay.class);
		assertSameCurrentSlice(imp, imageDisplay);

		int channelPos = 1;
		imageDisplay.getActiveView().setPosition(channelPos, Axes.CHANNEL);
		imageDisplay.update();
		assertSameCurrentSlice(imp, imageDisplay);

		int slicePos = 3;
		imageDisplay.getActiveView().setPosition(slicePos, Axes.Z);
		imageDisplay.update();
		assertSameCurrentSlice(imp, imageDisplay);

		int framePos = 0;
		imageDisplay.getActiveView().setPosition(framePos, Axes.TIME);
		imageDisplay.update();
		assertSameCurrentSlice(imp, imageDisplay);
	}

	@Test
	@Ignore
	// TODO this direction currently doesn't work
	// see https://github.com/imagej/imagej-legacy/issues/231#issuecomment-633690721
	public void testImagePlusToImageDisplaySynchronization() {
		ImagePlus imp = (ImagePlus) createImagePlus();
		imp.show();
		ImageDisplay imageDisplay = convertService.convert(imp, ImageDisplay.class);
		assertSameCurrentSlice(imp, imageDisplay);

		int channelPos = 1;
		imp.setC(channelPos + 1);
		imp.updateAndDraw();
		assertSameCurrentSlice(imp, imageDisplay);

		int slicePos = 3;
		imp.setZ(slicePos + 1);
		imp.updateAndDraw();
		assertSameCurrentSlice(imp, imageDisplay);

		int framePos = 0;
		imp.setT(framePos + 1);
		imp.updateAndDraw();
		assertSameCurrentSlice(imp, imageDisplay);
	}

	// TODO --- Synchronization of overlay, ROI etc.

	// --- ImageProcessor => * ---
	
	@Test
	@Ignore
	// TODO this currently doesn't work
	public void testImageProcessorToImg() {
		Object ip = ((ImagePlus) createImagePlus()).getProcessor();
		Img<?> img = convertService.convert(ip, Img.class);
		assertNotNull(img);
	}

	// -- Helper methods --

	private void assertImagesEqual(final Object o, final ImageDisplay display) {
		assertEquals(1, display.size());
		assertImagesEqual(o, (Dataset) display.get(0).getData());
	}

	@SuppressWarnings("unchecked")
	private void assertImagesEqual(final Object o, final Dataset d) {
		assertImagesEqual(o, (ImgPlus<RealType<?>>) d.getImgPlus());
	}

	private void assertImagesEqual(final Object o, final ImgPlus<RealType<?>> imgPlus) {
		final ImagePlus imp = (ImagePlus) o;
		assertEquals(imp.getTitle(), imgPlus.getName());
		assertEquals(imp.getWidth(), imgPlus.dimension(imgPlus.dimensionIndex(Axes.X)));
		assertEquals(imp.getHeight(), imgPlus.dimension(imgPlus.dimensionIndex(Axes.Y)));
		assertEquals(imp.getNChannels(), imgPlus.dimension(imgPlus.dimensionIndex(Axes.CHANNEL)));
		assertEquals(imp.getNSlices(), imgPlus.dimension(imgPlus.dimensionIndex(Axes.Z)));
		assertEquals(imp.getNFrames(), imgPlus.dimension(imgPlus.dimensionIndex(Axes.TIME)));

		assertImagesEqual(o, (Img<RealType<?>>) imgPlus);
	}

	/**
	 * Test if an ImageJ1 ImagePlus is equal to an ImageJ2/ImgLib2 Img.
	 * We test equality of dimensions, as well as intensities at each position.
	 * 
	 * @param o an {@code ImagePlus}, in fact.
	 * @param img the {@code Img} to be tested. We assume 5 dimensions in XYCZT order.
	 */
	private void assertImagesEqual(final Object o, final Img<RealType<?>> img) {
		final ImagePlus imp = (ImagePlus) o;
		assertEquals(imp.getWidth(), img.dimension(0));
		assertEquals(imp.getHeight(), img.dimension(1));
		assertEquals(imp.getNChannels(), img.dimension(2));
		assertEquals(imp.getNSlices(), img.dimension(3));
		assertEquals(imp.getNFrames(), img.dimension(4));
		final RandomAccess<RealType<?>> ra = img.randomAccess();
		for (int t = 0; t < imp.getNFrames(); t++) {
			ra.setPosition(t, 4);
			for (int z = 0; z < imp.getNSlices(); z++) {
				ra.setPosition(z, 3);
				for (int c = 0; c < imp.getNChannels(); c++) {
					ra.setPosition(c, 2);
					final ImageProcessor ip = imp.getStack().getProcessor(imp
						.getStackIndex(c + 1, z + 1, t + 1));
					for (int y = 0; y < imp.getHeight(); y++) {
						ra.setPosition(y, 1);
						for (int x = 0; x < imp.getWidth(); x++) {
							ra.setPosition(x, 0);
							assertEquals(ip.get(x, y), ra.get().getRealDouble(), 0);
						}
					}
				}
			}
		}
	}

	private void assertCalibrationEqual(Object o, ImageDisplay display) {
		assertCalibrationEqual(o, (Dataset) display.get(0).getData());
	}

	private void assertCalibrationEqual(Object o, Dataset dataset) {
		assertCalibrationEqual(o, dataset.getImgPlus());
	}

	@SuppressWarnings("rawtypes")
	private void assertCalibrationEqual(Object o, ImgPlus imgPlus) {
		final ImagePlus imp = (ImagePlus) o;
		Calibration cal = imp.getCalibration();
		assertEquals(cal.pixelWidth, imgPlus.averageScale(imgPlus.dimensionIndex(Axes.X)), 0);
		assertEquals(cal.pixelHeight, imgPlus.averageScale(imgPlus.dimensionIndex(Axes.Y)), 0);
		assertEquals(cal.pixelDepth, imgPlus.averageScale(imgPlus.dimensionIndex(Axes.Z)), 0);
	}

	private void assertSameCurrentSlice(Object o, ImageDisplay imageDisplay) {
		final ImagePlus imp = (ImagePlus) o;
		int[] stackPos = imp.convertIndexToPosition(imp.getCurrentSlice());
		assertEquals(stackPos[0] - 1, imageDisplay.getActiveView().getLongPosition(Axes.CHANNEL));
		assertEquals(stackPos[1] - 1, imageDisplay.getActiveView().getLongPosition(Axes.Z));
		assertEquals(stackPos[2] - 1, imageDisplay.getActiveView().getLongPosition(Axes.TIME));
	}

	/**
	 * NB: we need to return {@code Object} here, because the class can't be
	 * initialized with {@code ImagePlus} in the method signature.
	 * 
	 * @return ImagePlus to test
	 */
	private Object createImagePlus() {
		final int width = 128, height = 64, slices = 5, channels = 2, frames = 3;
		ImagePlus imp = IJ.createImage("gradient", "16-bit ramp", width, height, channels, slices, frames);
		Calibration cal = new Calibration();
		cal.pixelWidth = 0.12;
		cal.pixelHeight = 0.13;
		cal.pixelDepth = 0.9;
		imp.setCalibration(cal);
		return imp;
	}

}
