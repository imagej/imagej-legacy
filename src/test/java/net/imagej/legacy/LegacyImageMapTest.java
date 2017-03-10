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

package net.imagej.legacy;

import static org.junit.Assert.assertTrue;
import ij.ImagePlus;
import ij.gui.NewImage;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;

import org.junit.Test;
import org.scijava.Context;

/**
 * Unit tests for {@link LegacyImageMap}.
 * 
 * @author Barry DeZonia
 */
public class LegacyImageMapTest {

	static {
		/*
		 * We absolutely require that the LegacyInjector did its job before we
		 * use the ImageJ 1.x classes here, in case the LegacyService tests did
		 * not run yet, so that the classes are properly patched before use.
		 * 
		 * Just loading the class is not enough; it will not get initialized. So
		 * we call the preinit() method just to force class initialization (and
		 * thereby the LegacyInjector to patch ImageJ 1.x).
		 */
		LegacyInjector.preinit();
	}

	// -- instance variables --

	// TODO - fix tests
//	@SuppressWarnings("unchecked")
//	private final ImageJ context = new ImageJ(EventService.class);
//	private final LegacyImageMap map = new LegacyImageMap(context
//		.getService(EventService.class));

	// -- private interface --

	private void fill(final Dataset ds) {
		final Cursor<? extends RealType<?>> cursor = ds.getImgPlus().cursor();
		int val = 0;
		while (cursor.hasNext()) {
			cursor.next();
			cursor.get().setReal(val++ % 256);
		}
	}

	// -- public tests --

	@Test
	public void testRegisterDataset() {
		final Context context = new Context(DatasetService.class);
		final DatasetService datasetService =
			context.getService(DatasetService.class);

		Dataset ds0;
		final AxisType[] axes =
			new AxisType[] { Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME };

		// register a gray dataset
		ds0 =
			datasetService.create(new long[] { 1, 2, 3, 4, 5 }, "temp", axes, 16,
				false, false);
		fill(ds0);
		ImgPlus<? extends RealType<?>> imgPlus = ds0.getImgPlus();
		imgPlus.setAxis(new DefaultLinearAxis(Axes.X, 5, 1), 0);
		imgPlus.setAxis(new DefaultLinearAxis(Axes.Y, 6, 2), 0);
		imgPlus.setAxis(new DefaultLinearAxis(Axes.CHANNEL, 1, 0), 0);
		imgPlus.setAxis(new DefaultLinearAxis(Axes.Z, 7, 3), 0);
		imgPlus.setAxis(new DefaultLinearAxis(Axes.TIME, 8, 0), 0);
		// CTR FIXME - Fix comparison tests.
//		imp1 = map.registerDataset(ds0);
//		Utils.testSame(ds0, imp1);

		// try again
		// CTR FIXME - Fix comparison tests.
//		imp2 = map.registerDataset(ds0);
//		assertSame(imp1, imp2);
//		Utils.testSame(ds0, imp2);

		// register a color dataset
		ds0 =
			datasetService.create(new long[] { 1, 2, 3, 4, 5 }, "temp", axes, 8,
				false, false);
		fill(ds0);
		imgPlus = ds0.getImgPlus();
		imgPlus.setAxis(new DefaultLinearAxis(Axes.X, 5, 1), 0);
		imgPlus.setAxis(new DefaultLinearAxis(Axes.Y, 6, 2), 0);
		imgPlus.setAxis(new DefaultLinearAxis(Axes.CHANNEL, 1, 0), 0);
		imgPlus.setAxis(new DefaultLinearAxis(Axes.Z, 7, 3), 0);
		imgPlus.setAxis(new DefaultLinearAxis(Axes.TIME, 8, 0), 0);
		ds0.setRGBMerged(true);
		// CTR FIXME - Fix comparison tests.
//		imp1 = map.registerDataset(ds0);
//		Utils.testColorSame(ds0, imp1);

		// try again
		// CTR FIXME - Fix comparison tests.
//		imp2 = map.registerDataset(ds0);
//		assertSame(imp1, imp2);
//		Utils.testColorSame(ds0, imp2);
		assertTrue(true);
	}

	@Test
	public void testRegisterLegacyImage() {
		ImagePlus imp;
		int c, z, t;

		// dataset that was not existing
		c = 3;
		z = 4;
		t = 5;
		imp =
			NewImage.createShortImage("name", 4, 7, c * z * t, NewImage.FILL_RAMP);
		imp.setDimensions(c, z, t);
		// CTR FIXME - Fix comparison tests.
//		ds0 = map.registerLegacyImage(imp);
//		Utils.testSame(ds0,imp);

		// dataset that exists and no changes
		// CTR FIXME - Fix comparison tests.
//		imgPlus0 = ds0.getImgPlus();
//		ds1 = map.registerLegacyImage(imp);
//		assertSame(ds0,ds1);
//		assertSame(imgPlus0,ds1.getImgPlus());
//		Utils.testSame(ds1,imp);
		assertTrue(true);
	}

	@Test
	public void testReconciliation() {
		/* TODO - fix initialization after legacy layer refactor
		final DatasetHarmonizer harmonizer =
			new DatasetHarmonizer(map.getTranslator());
		ImagePlus imp;
		final Dataset ds0, ds1;
		final ImgPlus<?> imgPlus0;
		int c, z, t;

		// dataset that was not existing
		c = 3;
		z = 4;
		t = 5;
		imp =
			NewImage.createShortImage("name", 4, 7, c * z * t, NewImage.FILL_RAMP);
		imp.setDimensions(c, z, t);
		*/

		// CTR FIXME - Fix comparison tests.
//		ds0 = map.registerLegacyImage(imp);
//		//map.reconcileDifferences(ds0, imp);
//		Utils.testSame(ds0,imp);

		// dataset that exists and no changes
		// CTR FIXME - Fix comparison tests.
//		imgPlus0 = ds0.getImgPlus();
//		ds1 = map.registerLegacyImage(imp);
//		//map.reconcileDifferences(ds1, imp);
//		assertSame(ds0,ds1);
//		assertSame(imgPlus0,ds1.getImgPlus());
//		Utils.testSame(ds1,imp);

		// dataset exists and some changes

		// CTR FIXME - Fix comparison tests.

//		//   some change and not backed by planar access
//		c = 3; z = 4; t = 5;
//		CellImgFactory<UnsignedByteType> factory = new CellImgFactory<UnsignedByteType>();
//		CellImg<UnsignedByteType,?> cellImg = factory.create(new long[]{4,7,c,z,t}, new UnsignedByteType());
//		Axis[] ij1Axes = new Axis[]{ Axes.X, Axes.Y, Axes.CHANNEL, Axes.Z, Axes.TIME };
//		ImgPlus<UnsignedByteType> cellImgPlus = new ImgPlus<UnsignedByteType>(cellImg,"temp",ij1Axes,new double[]{4,5,1,7,8});
//		ds0 = new Dataset(cellImgPlus);
//		imp = map.registerDataset(ds0);
//		//map.reconcileDifferences(ds0, imp);
//		Utils.testSame(ds0,imp);
//		assertFalse(0xff == imp.getProcessor().get(0,0));
//		imp.getProcessor().set(0, 0, 0xff);  // MAKE THE CHANGE
//		imgPlus0 = ds0.getImgPlus();
//		ds1 = map.registerLegacyImage(imp);
//		harmonizer.registerType(imp);
//		harmonizer.updateDataset(ds1, imp);
//		assertSame(ds0,ds1);
//		//assertNotSame(imgPlus0,ds1.getImgPlus());
//		
//		//   some change and is color
//		c = 1; z = 4; t = 5;
//		imp = NewImage.createRGBImage("name", 4, 7, c*z*t, NewImage.FILL_RAMP);
//		imp.setDimensions(c, z, t);
//		ds0 = map.registerLegacyImage(imp);
//		//map.reconcileDifferences(ds0, imp);
//		Utils.testColorSame(ds0,imp);
//		assertFalse(0xffffffff == imp.getProcessor().get(0,0));
//		imp.getProcessor().set(0, 0, 0xffffffff);  // MAKE THE CHANGE
//		imgPlus0 = ds0.getImgPlus();
//		ds1 = map.registerLegacyImage(imp);
//		harmonizer.registerType(imp);
//		harmonizer.updateDataset(ds1, imp);
//		assertSame(ds0,ds1);
//		assertNotSame(imgPlus0,ds1.getImgPlus());
//		Utils.testColorSame(ds1,imp);
//
//		//   some change - dimensions changed
//		c = 3; z = 4; t = 5;
//		imp = NewImage.createFloatImage("name", 4, 7, c*z*t, NewImage.FILL_RAMP);
//		imp.setDimensions(c, z, t);
//		ds0 = map.registerLegacyImage(imp);
//		//map.reconcileDifferences(ds0, imp);
//		Utils.testSame(ds0,imp);
//		imp.getStack().deleteLastSlice();  // MAKE THE CHANGE
//		imp.setStack(imp.getStack());
//		imgPlus0 = ds0.getImgPlus();
//		ds1 = map.registerLegacyImage(imp);
//		harmonizer.registerType(imp);
//		harmonizer.updateDataset(ds1, imp);
//		assertSame(ds0,ds1);
//		assertNotSame(imgPlus0,ds1.getImgPlus());
//		Utils.testSame(ds1,imp);
//		
//		// some change - metadata only
//		c = 3; z = 4; t = 5;
//		imp = NewImage.createByteImage("name", 4, 7, c*z*t, NewImage.FILL_RAMP);
//		imp.setDimensions(c, z, t);
//		ds0 = map.registerLegacyImage(imp);
//		//map.reconcileDifferences(ds0, imp);
//		Utils.testSame(ds0,imp);
//		imp.getCalibration().pixelDepth = 99;  // MAKE THE CHANGE
//		imgPlus0 = ds0.getImgPlus();
//		ds1 = map.registerLegacyImage(imp);
//		harmonizer.registerType(imp);
//		harmonizer.updateDataset(ds1, imp);
//		assertSame(ds0,ds1);
//		assertSame(imgPlus0,ds1.getImgPlus());
//		Utils.testSame(ds1,imp);
//		
//		// some change - pixel data that is correct by reference
//		c = 2; z = 4; t = 5;
//		imp = NewImage.createByteImage("name", 4, 7, c*z*t, NewImage.FILL_RAMP);
//		imp.setDimensions(c, z, t);
//		ds0 = map.registerLegacyImage(imp);
//		//map.reconcileDifferences(ds0, imp);
//		Utils.testSame(ds0,imp);
//		assertFalse(100 == imp.getProcessor().get(0,0));
//		imp.getProcessor().set(0, 0, 100);  // MAKE THE CHANGE
//		imgPlus0 = ds0.getImgPlus();
//		ds1 = map.registerLegacyImage(imp);
//		harmonizer.registerType(imp);
//		harmonizer.updateDataset(ds1, imp);
//		assertSame(ds0,ds1);
//		assertSame(imgPlus0,ds1.getImgPlus());
//		Utils.testSame(ds1,imp);
		assertTrue(true);
	}
}
