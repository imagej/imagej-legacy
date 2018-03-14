package net.imagej.legacy.translate;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.junit.Ignore;
import org.junit.Test;
import org.scijava.Context;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImagePlusCreatorTest
{

	private Context context = new Context();
	private GrayImagePlusCreator creator = new GrayImagePlusCreator( context );
	private DatasetService datasetService = context.service( DatasetService.class );

	@Test
	public void testBytes() {
		testArrayImage( UnsignedByteType::new, ByteProcessor.class );
	}

	@Test
	public void testFloats() {
		testArrayImage( FloatType::new, FloatProcessor.class );
	}

	@Test
	public void testShorts() {
		testArrayImage( UnsignedShortType::new, ShortProcessor.class );
	}

	@Ignore("currently broken")
	@Test
	public void testSignedShorts() {
		testArrayImage( ShortType::new, FloatProcessor.class );
	}

	@Test
	public void testByteCellImage() {
		testCellImage( UnsignedByteType::new, ByteProcessor.class );
	}

	@Test
	public void testFloatCellImage() {
		testCellImage( FloatType::new, FloatProcessor.class );
	}

	@Test
	public void testShortCellImage() {
		testCellImage( UnsignedShortType::new, ShortProcessor.class );
	}

	@Test
	public void testBytePlanarImage() {
		testPlanarImage( UnsignedByteType::new, ByteProcessor.class );
	}

	@Test
	public void testFloatPlanarImage() {
		testPlanarImage( FloatType::new, FloatProcessor.class );
	}

	@Test
	public void testShortPlanarImage() {
		testPlanarImage( UnsignedShortType::new, ShortProcessor.class );
	}

	@Test
	public void testColor() {
		Img< UnsignedByteType > image = ArrayImgs.unsignedBytes(new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }, 2, 2, 3);
		AxisType[] axes = { Axes.X, Axes.Y, Axes.CHANNEL };
		ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( image, "image", axes );
		ColorImagePlusCreator creator = new ColorImagePlusCreator( context );
		Dataset ds = datasetService.create( imgPlus );
		ds.setRGBMerged( true );
		ImagePlus imagePlus = creator.createLegacyImage( ds );
		ImageProcessor processor = imagePlus.getProcessor();
		assertTrue( processor instanceof ColorProcessor );
		IntStream.of((int[]) processor.getPixels()).mapToObj( Integer::toHexString ).forEach( System.out::println );
		assertEquals( 0xff010509, processor.getPixel(0,0) );
		assertEquals( 0xff02060a, processor.getPixel(1,0) );
		assertEquals( 0xff03070b, processor.getPixel(0,1) );
		assertEquals( 0xff04080c, processor.getPixel(1,1) );
	}

	@Test
	public void testCompositeImage() {
		byte[] values = new byte[] { 1, 2, 3, 4 };
		Img< UnsignedByteType > image = ArrayImgs.unsignedBytes(values, 2, 1, 2);
		AxisType[] axes = { Axes.X, Axes.Y, Axes.CHANNEL };
		ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( image, "image", axes );
		imgPlus.setCompositeChannelCount( 7 );
		Dataset ds = datasetService.create( imgPlus );
		assertTrue( LegacyUtils.isColorCompatible( ds ) );
		ColorImagePlusCreator creator = new ColorImagePlusCreator( context );
		ImagePlus imagePlus = creator.createLegacyImage( ds );
		assertTrue( imagePlus instanceof CompositeImage );
		ImageProcessor processor = imagePlus.getProcessor();
		assertArrayEquals( Arrays.copyOf( values, 2 ) , ((byte[]) processor.getPixels()) );
	}

	@Ignore("not working yet")
	@Test
	public void testCompositeImageLong() {
		long[] values = { 1, 2, 3, 4 };
		Img< LongType > image = ArrayImgs.longs(values, 2, 1, 2);
		AxisType[] axes = { Axes.X, Axes.Y, Axes.CHANNEL };
		ImgPlus< LongType > imgPlus = new ImgPlus<>( image, "image", axes );
		imgPlus.setCompositeChannelCount( 7 );
		Dataset ds = datasetService.create( imgPlus );
		assertTrue( LegacyUtils.isColorCompatible( ds ) );
		ColorImagePlusCreator creator = new ColorImagePlusCreator( context );
		ImagePlus imagePlus = creator.createLegacyImage( ds );
		assertTrue( imagePlus instanceof CompositeImage );
	}

	private < T extends NativeType<T> & RealType<T> > void testArrayImage( Supplier< T > typeConstructor, Class< ? > processorClass )
	{
		testImgPlusCreator( typeConstructor, processorClass, new ArrayImgFactory<>() );
	}

	private < T extends NativeType<T> & RealType<T> > void testCellImage( Supplier< T > typeConstructor, Class< ? > processorClass )
	{
		testImgPlusCreator( typeConstructor, processorClass, new CellImgFactory<>() );
	}

	private < T extends NativeType<T> & RealType<T> > void testPlanarImage( Supplier< T > typeConstructor, Class< ? > processorClass )
	{
		testImgPlusCreator( typeConstructor, processorClass, new PlanarImgFactory<>() );
	}

	private < T extends NativeType< T > & RealType< T > > void testImgPlusCreator( Supplier< T > typeConstructor, Class< ? > processorClass, ImgFactory< T > factory )
	{
		float[] expectedPixels = { 1, 2, 3, 4 };
		long[] dimensions = { 2, 2 };
		Img< T > image = createImg( typeConstructor, expectedPixels, dimensions, factory );
		ImagePlus imagePlus = creator.createLegacyImage( datasetService.create( image ) );
		ImageProcessor processor = imagePlus.getProcessor();
		assertTrue( processorClass.isInstance( processor ) );
		float[] actualPixels = pixelsAsFloatArray( processor );
		assertArrayEquals( expectedPixels, actualPixels, 0f);
	}

	private float[] pixelsAsFloatArray( ImageProcessor processor )
	{
		return ( float[] ) processor.toFloat( 0, null ).getPixels();
	}

	private < T extends NativeType< T > & RealType< T > > Img< T > createImg( Supplier< T > aNew, float[] expectedPixels, long[] dimensions, ImgFactory< T > factory )
	{
		Img< T > image = factory.create( aNew, dimensions );
		int i = 0;
		for( T pixel : image)
			pixel.setReal( expectedPixels[i++] );
		return image;
	}
}
