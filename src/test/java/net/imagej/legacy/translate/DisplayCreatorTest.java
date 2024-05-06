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

package net.imagej.legacy.translate;

import static org.junit.Assert.assertEquals;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.display.ImageDisplay;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.test.ImgLib2Assert;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;

/**
 * Tests {@link DisplayCreator}.
 *
 * @author Matthias Arzt
 */
public class DisplayCreatorTest
{
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

	/** NB: We need this subclass to make the LegacyInjector work. */
	private static class SubClass {
		private static Dataset toDataset( Context context, ImagePlus image )
		{
			ImageDisplay display = new DisplayCreator( context ).createDisplay( image );
			return ( Dataset ) display.getActiveView().getData();
		}

		private static ImagePlus createImagePlus( int c, int z, int t, ImageProcessor... imageProcessors )
		{
			ImageStack imageStack = new ImageStack( imageProcessors[0].getWidth(), imageProcessors[0].getHeight() );
			for( ImageProcessor imageProcessor : imageProcessors )
				imageStack.addSlice( imageProcessor );
			ImagePlus image = new ImagePlus( "image", imageStack );
			image.setStack( imageStack, c, z, t );
			return image;
		}

		private static void testConversion(Context context, RandomAccessibleInterval<? extends RealType<?>> expected, ImageProcessor ip)
		{
			ImagePlus image = new ImagePlus( "image", ip );
			testConversion( context, expected, image );
		}

		private static void testConversion( Context context, RandomAccessibleInterval< ? extends RealType< ? > > expected, ImagePlus image )
		{
			Dataset dataset = toDataset( context, image );
			assertEquals( expected.randomAccess().get().getClass(), dataset.getType().getClass() );
			ImgLib2Assert.assertImageEqualsRealType(expected, dataset, 0);
		}

		private static ImagePlus createColorImagePlus( int width, int height, int c, int z, int t, int[][] pixels )
		{
			return createImagePlus( c, z, t, Stream.of(pixels).map( p -> new ColorProcessor( width, height, p )).toArray(ImageProcessor[]::new));
		}

		private static ImagePlus createByteImagePlus( int width, int height, int c, int z, int t, byte[][] pixels )
		{
			return createImagePlus( c, z, t, Stream.of(pixels).map( p -> new ByteProcessor( width, height, p )).toArray(ImageProcessor[]::new));
		}
	}

	@Test
	public void testBytesWrapping()
	{
		int width = 10, height = 10;
		byte[] pixels = randomBytes( width * height );
		SubClass.testConversion(context, ArrayImgs.unsignedBytes( pixels, width, height ), new ByteProcessor( width, height, pixels ));
	}

	@Test
	public void testShortWrapping()
	{
		int width = 10, height = 10;
		short[] pixels = randomShorts( width * height );
		SubClass.testConversion(context, ArrayImgs.unsignedShorts( pixels, width, height ), new ShortProcessor( width, height, pixels, null ));
	}

	@Test
	public void testFloatWrapping()
	{
		int width = 10, height = 10;
		float[] pixels = randomFloats( width * height );
		SubClass.testConversion(context, ArrayImgs.floats( pixels, width, height ), new FloatProcessor( width, height, pixels ));
	}

	@Test
	public void testColorWrapping()
	{
		int width = 2, height = 2;
		ColorProcessor ip = new ColorProcessor( width, height, new int[] { 0x010203, 0x040506, 0x070809, 0x0a0b0c } );
		RandomAccessibleInterval< UnsignedByteType > expected = ArrayImgs.unsignedBytes( new byte[]{1,4,7,10,2,5,8,11,3,6,9,12}, width, height, 3 );
		SubClass.testConversion(context, expected, ip);
	}

	@Test
	public void testColorStackWrapping()
	{
		int width = 2, height = 1, channels = 2;
		ImagePlus image = SubClass.createColorImagePlus( width, height, channels, 1, 1, new int[][] { { 0x010203, 0x040506 }, { 0x070809, 0x0a0b0c } } );
		RandomAccessibleInterval< UnsignedByteType > expected = ArrayImgs.unsignedBytes( new byte[]{1,4,2,5,3,6,7,10,8,11,9,12}, width, height, 3, channels );
		SubClass.testConversion( context, expected, image );
	}

	@Test
	public void testColorStackWrapping2()
	{
		int width = 2, height = 1, slices = 2;
		ImagePlus image = SubClass.createColorImagePlus( width, height, 1, slices, 1, new int[][] { { 0x010203, 0x040506 }, { 0x070809, 0x0a0b0c } } );
		RandomAccessibleInterval< UnsignedByteType > expected = ArrayImgs.unsignedBytes( new byte[]{1,4,2,5,3,6,7,10,8,11,9,12}, width, height, 3, slices );
		SubClass.testConversion( context, expected, image );
	}

	@Test
	public void testBytes4dWrapping()
	{
		int x = 2, y = 5, z = 3, c = 7;
		byte[][] pixels = IntStream.range(0, z * c).mapToObj( i -> randomBytes( x * y ) ).toArray(byte[][]::new);
		ImagePlus image = SubClass.createByteImagePlus( x, y, c, z, 1, pixels );
		RandomAccessibleInterval< UnsignedByteType > expected = ArrayImgs.unsignedBytes( concat( pixels ), x, y, c, z );
		SubClass.testConversion( context, expected, image );
	}

	private float[] randomFloats( int size )
	{
		Random random = new Random(42);
		float[] result = new float[ size ];
		for ( int i = 0; i < size; i++ )
			result[ i ] = random.nextFloat( );
		return result;
	}

	private short[] randomShorts( int size )
	{
		Random random = new Random(42);
		short[] result = new short[ size ];
		for ( int i = 0; i < size; i++ )
			result[ i ] = (short) random.nextInt( );
		return result;
	}

	private static byte[] randomBytes( int size )
	{
		byte[] result = new byte[size];
		new Random().nextBytes( result );
		return result;
	}

	private static byte[] concat(byte[]... arrays) {
		return Stream.of(arrays).reduce( new byte[0], DisplayCreatorTest::addAll );
	}

	public static byte[] addAll( final byte[] a, byte[] b )
	{
		byte[] joinedArray = Arrays.copyOf( a, a.length + b.length );
		System.arraycopy( b, 0, joinedArray, a.length, b.length );
		return joinedArray;
	}

	@Test
	public void testCalibration() {
		int frameInterval = 42;
		ImagePlus image = NewImage.createByteImage("test", 10, 20, 30, NewImage.FILL_BLACK);
		image.setDimensions(1, 1, 30);
		image.getCalibration().frameInterval = frameInterval;
		Dataset result = SubClass.toDataset(context, image);
		assertEquals(frameInterval, result.averageScale(result.dimensionIndex(Axes.TIME)), 0);
	}

	@Test
	public void testName() {
		String title = "Hello World!";
		ImagePlus image = NewImage.createByteImage(title, 10, 20, 30, NewImage.FILL_BLACK);
		Dataset result = SubClass.toDataset(context, image);
		assertEquals(title, result.getName());
	}
}
