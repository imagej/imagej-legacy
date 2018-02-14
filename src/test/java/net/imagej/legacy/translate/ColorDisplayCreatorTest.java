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

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import net.imagej.Dataset;
import net.imagej.display.ImageDisplay;
import net.imagej.legacy.translate.ColorDisplayCreator;
import net.imagej.legacy.translate.GrayDisplayCreator;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Intervals;
import org.junit.Test;
import org.scijava.Context;

import java.util.Random;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ColorDisplayCreator}.
 *
 * @author Matthias Arzt
 */
public class ColorDisplayCreatorTest
{
	static {
		LegacyInjector.preinit();
	}

	@Test
	public void testColorWrapping() throws ExecutionException, InterruptedException
	{
		int width = 10;
		int height = 10;
		int[] pixels = randomInts( width * height );
		ImagePlus image = new ImagePlus( "image", new ColorProcessor( width, height, pixels ) );
		ImageDisplay display = new ColorDisplayCreator( new Context() ).createDisplay( image );
		Dataset dataset = ( Dataset ) display.getActiveView().getData();
		byte[] actualPixels = getBytes( dataset );
		assertEquals( UnsignedByteType.class, dataset.getType().getClass() );
		assertTrue( Intervals.equals( Intervals.createMinSize( 0, 0, 0, width, height, 3 ), dataset ) );
		assertArrayEquals( pixels, bytesTwoRgbInts(actualPixels) );
	}

	private int[] bytesTwoRgbInts( byte[] actualPixels )
	{
		int numInts = actualPixels.length / 3;
		int doubledNumInts = numInts * 2;
		int[] result = new int[numInts];
		for ( int i = 0; i < numInts; i++ )
			result[i] = ARGBType.rgba( actualPixels[i], actualPixels[i + numInts ], actualPixels[i + doubledNumInts], 0 );
		return result;
	}

	private int[] randomInts( int size )
	{
		Random random = new Random(42);
		int[] result = new int[ size ];
		for ( int i = 0; i < size; i++ )
		{
			result[i] = random.nextInt( 0xffffff );
		}
		return result;
	}

	private static byte[] getBytes( RandomAccessibleInterval< ? extends RealType< ? > > dataset )
	{
		byte[] bytes = new byte[(int) Intervals.numElements(dataset)];
		copy( dataset, ArrayImgs.bytes( bytes, Intervals.dimensionsAsLongArray(dataset) ) );
		return bytes;
	}

	private static void copy( RandomAccessibleInterval< ? extends RealType< ? > > dataset, RandomAccessibleInterval< ? extends RealType<?> > arrayImg )
	{
		LoopBuilder.setImages( dataset, arrayImg ).forEachPixel( (i,o) -> o.setReal( i.getRealDouble() ) );
	}

}
