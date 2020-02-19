/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2020 ImageJ developers.
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

package net.imagej.test;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.operators.ValueEquals;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.junit.Assert;

public class AssertImgs
{
	// TODO generalize & move to imglib2
	public static <T> void assertImageEquals(
			RandomAccessibleInterval< ? extends ValueEquals< T > > expected,
			RandomAccessibleInterval< T > actual )
	{
		pairedForEach( expected, actual, ValueEquals::valueEquals );
	}

	public static void assertRealTypeImageEquals(
			RandomAccessibleInterval< ? extends RealType< ? > > expected,
			RandomAccessibleInterval< ? extends RealType< ? > > actual )
	{
		assertImageEquals( expected, actual, (e, a) -> e.getRealDouble() == a.getRealDouble() );
	}

	public static <T, U> void assertImageEquals(
			RandomAccessibleInterval< T > expected, RandomAccessibleInterval< U > actual, BiPredicate< ? super T, ? super U > equals )
	{
		pairedForEach( expected, actual, (e, a) -> Assert.assertTrue( equals.test( e, a ) ) );
	}

	private static void assertIntervalEquals( Interval expected, Interval actual )
	{
		if ( ! Intervals.equals( expected, actual ) )
			fail("intervals differ, expected=" + toString( expected ) + " actual=" + toString( actual ) );
	}

	private static String toString( Interval actual )
	{
		return "{min=" + Arrays.toString( Intervals.minAsLongArray( actual ) )
				+ ",size=" + Arrays.toString( Intervals.dimensionsAsLongArray( actual ) ) + "}";
	}

	private static < A, B > void pairedForEach( RandomAccessibleInterval< A > expected, RandomAccessibleInterval< B > actual, BiConsumer<A, B> pairConsumer )
	{
		assertIntervalEquals( expected, actual );
		Views.interval(Views.pair(expected, actual), expected).forEach ( p -> pairConsumer.accept( p.getA(), p.getB() ) );
	}

}
