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

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.scijava.Context;

/**
 * Benchmark for ImagePlusCreator.
 * Compare the performance of ImagePlusCreator on different image types and sizes.
 *
 * @author Matthias Arzt
 */
@State( Scope.Thread )
public class ImagePlusCreatorBenchmark
{
	private final Context context = new Context();
	private final DatasetService datasetService = context.service( DatasetService.class );
	private final ImagePlusCreator creator = new ImagePlusCreator( context );

	private final long[] smallDims = { 10, 10, 10 };
	private final long[] deepDims = { 10, 10, 1000000 };
	private final long[] cubicDims = { 1000, 1000, 1000 };

	private final Dataset smallCellImage = makeDataset( createCellImg( deepDims ) );
	private final Dataset deepCellImage = makeDataset( createCellImg( deepDims ) );
	private final Dataset cubicCellImage = makeDataset( createCellImg( cubicDims ) );
	private final Dataset smallPlanarImg = makeDataset( PlanarImgs.unsignedBytes( smallDims ) );
	private final Dataset cubicPlanarImg = makeDataset( PlanarImgs.unsignedBytes( cubicDims ) );
	private final Dataset deepPlanarImg = makeDataset( PlanarImgs.unsignedBytes( deepDims ) );
	private final Dataset small2dArrayImg = makeDataset( ArrayImgs.unsignedBytes( 10, 10 ) );
	private final Dataset big2dArrayImg = makeDataset( ArrayImgs.unsignedBytes( 10000, 10000 ) );

	@Benchmark
	public void testSmallCellImg() {
		creator.createLegacyImage( smallCellImage );
	}

	@Benchmark
	public void testDeepCellImg() {
		creator.createLegacyImage( deepCellImage );
	}

	@Benchmark
	public void testCubicCellImg() {
		creator.createLegacyImage( cubicCellImage );
	}

	@Benchmark
	public void testSmallPlanarImg() {
		creator.createLegacyImage( smallPlanarImg );
	}

	@Benchmark
	public void testCubicPlanarImg() {
		creator.createLegacyImage( cubicPlanarImg );
	}

	@Benchmark
	public void testDeepPlanarImg() {
		creator.createLegacyImage( deepPlanarImg );
	}

	@Benchmark
	public void testSmall2dArrayImg() {
		creator.createLegacyImage( small2dArrayImg );
	}

	@Benchmark
	public void testLarge2dArrayImg() {
		creator.createLegacyImage( big2dArrayImg );
	}

	private Dataset makeDataset( Img< UnsignedByteType > deepPlanarImg )
	{
		AxisType[] axes = { Axes.X, Axes.Y, Axes.Z };
		ImgPlus< UnsignedByteType > imgPlus = new ImgPlus<>( deepPlanarImg, "title", axes );
		return datasetService.create( imgPlus );
	}

	private Img<UnsignedByteType> createCellImg(long... dim) {
		return new CellImgFactory<>( new UnsignedByteType(  ) ).create( dim );
	}

	public static void main( final String... args ) throws RunnerException
	{
		final Options opt = new OptionsBuilder()
				.include( ImagePlusCreatorBenchmark.class.getSimpleName() )
				.forks( 0 )
				.warmupIterations( 4 )
				.measurementIterations( 8 )
				.warmupTime( TimeValue.milliseconds( 100 ) )
				.measurementTime( TimeValue.milliseconds( 100 ) )
				.build();
		new Runner( opt ).run();
	}
}
