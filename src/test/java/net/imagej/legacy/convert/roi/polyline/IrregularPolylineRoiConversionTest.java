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

package net.imagej.legacy.convert.roi.polyline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

import net.imagej.legacy.convert.roi.RoiUnwrappers.WrapperToPolygonRoiConverter;
import net.imglib2.roi.RealMaskRealInterval;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.scijava.Context;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;

/**
 * Tests converting {@link PolygonRoi} to {@link RealMaskRealInterval}.
 * <p>
 * Specifically, this tests the following conversions:
 * </p>
 * <ul>
 * <li>{@link Roi#FREELINE} with non-zero width to RealMaskRealInterval</li>
 * <li>{@link Roi#POLYLINE} with non-zero width to RealMaskRealInterval</li>
 * <li>Spline fitted {@link Roi#POLYLINE} to RealMaskRealInterval</li>
 * <li>Spline fitted {@link Roi#POLYLINE} with non-zero width to
 * RealMaskRealInterval</li>
 * </ul>
 *
 * @author Alison Walter
 */
public class IrregularPolylineRoiConversionTest {

	private PolygonRoi poly;
	private RealMaskRealInterval wrap;
	private PolygonRoi free;
	private RealMaskRealInterval freeWrap;
	private ConvertService convertService;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Before
	public void setup() {
		// 53, -27
		poly = new PolygonRoi(new float[] { 1.25f, 20, 50, 79 }, new float[] {
			1.25f, 20, -30, -1 }, Roi.POLYLINE);
		wrap = new IrregularPolylineRoiWrapper(poly);

		final int[] xf = new int[] { 143, 136, 128, 126, 124, 123, 122, 121, 120,
			118, 118, 117, 116, 116, 116, 115, 115, 115, 115, 115, 115, 115, 115, 116,
			116, 117, 117, 117, 118, 119, 119, 119, 120, 120, 121, 122, 122, 123, 124,
			125, 126, 127, 128, 129, 130, 131, 131, 132, 133, 132, 133, 135, 136, 137,
			138, 140, 141, 142, 143, 144, 144, 146, 147, 147, 148, 149, 150, 152, 153,
			153, 154, 156, 157, 158, 158, 158, 158, 158, 158, 158, 157, 157, 156, 155,
			155, 154, 154, 153, 153, 152, 151, 149, 149, 148, 147, 146, 146, 145, 144,
			143, 142, 141, 140, 140, 140, 140, 141, 141, 142, 143, 144, 144, 145, 146,
			147, 149, 150, 151, 152, 152, 153, 154, 155, 156, 157, 158, 159, 161, 162,
			166, 168, 169, 170, 171, 172, 172, 174, 175, 175, 176, 177, 177, 179, 180,
			180, 181, 182, 183, 183 };
		final int[] yf = new int[] { 37, 37, 34, 34, 34, 34, 34, 34, 35, 39, 40, 42,
			42, 44, 45, 45, 46, 47, 48, 49, 50, 51, 52, 54, 55, 57, 58, 59, 60, 61,
			62, 63, 63, 64, 65, 65, 66, 67, 67, 68, 69, 69, 70, 71, 71, 71, 72, 72,
			72, 72, 72, 72, 72, 72, 72, 73, 73, 73, 73, 73, 74, 74, 74, 75, 75, 76,
			76, 76, 76, 77, 78, 79, 80, 81, 82, 83, 85, 86, 87, 88, 89, 90, 91, 92,
			94, 95, 96, 96, 97, 98, 98, 99, 100, 100, 100, 101, 102, 102, 102, 102,
			103, 103, 103, 104, 105, 106, 106, 107, 107, 108, 108, 109, 109, 109, 109,
			110, 110, 110, 110, 111, 111, 111, 111, 111, 111, 111, 111, 111, 111, 113,
			113, 114, 114, 114, 115, 116, 118, 119, 120, 120, 121, 122, 123, 124, 125,
			125, 126, 128, 129 };
		free = new PolygonRoi(xf, yf, xf.length, Roi.FREELINE);
		freeWrap = new IrregularPolylineRoiWrapper(free);

		final Context context = new Context(ConvertService.class);
		convertService = context.service(ConvertService.class);
	}

	@After
	public void tearDown() {
		convertService.context().dispose();
	}

	// -- PolygonRoi to RealMaskRealInterval conversion tests --
	@Test
	public void
		testPolylineRoiToRealMaskRealIntervalConverterPolylineWithWidth()
	{
		poly.updateWideLine(10);
		final RealMaskRealInterval converted = convertService.convert(poly,
			RealMaskRealInterval.class);

		assertTrue(converted instanceof IrregularPolylineRoiWrapper);
	}

	@Test
	public void
		testPolylineRoiToRealMaskRealIntervalConverterFreelineWithWidth()
	{
		free.updateWideLine(12);
		final RealMaskRealInterval converted = convertService.convert(free,
			RealMaskRealInterval.class);

		assertTrue(converted instanceof IrregularPolylineRoiWrapper);
	}

	@Test
	public void
		testPolylineRoiToRealMaskRealIntervalConverterSplineFitPolyline()
	{
		poly.fitSpline();
		final RealMaskRealInterval converted = convertService.convert(poly,
			RealMaskRealInterval.class);

		assertTrue(converted instanceof IrregularPolylineRoiWrapper);
	}

	@Test
	public void
		testPolylineRoiToRealMaskRealIntervalConverterSplineFitPolylineWithWidth()
	{
		poly.fitSpline();
		poly.updateWideLine(5);
		final RealMaskRealInterval converted = convertService.convert(poly,
			RealMaskRealInterval.class);

		assertTrue(converted instanceof IrregularPolylineRoiWrapper);
	}

	// -- Wrapped PolygonRoi to PolygonRoi conversion tests --

	@Test
	public void testUnwrapConverterMatching() {
		free.updateWideLine(1.5f);
		final Converter<?, ?> fWidth = convertService.getHandler(freeWrap,
			PolygonRoi.class);
		assertTrue(fWidth instanceof WrapperToPolygonRoiConverter);

		poly.fitSpline();
		final Converter<?, ?> pSpline = convertService.getHandler(wrap,
			PolygonRoi.class);
		assertTrue(pSpline instanceof WrapperToPolygonRoiConverter);

		poly.updateWideLine(2.5f);
		final Converter<?, ?> pSplineWidth = convertService.getHandler(wrap,
			PolygonRoi.class);
		assertTrue(pSplineWidth instanceof WrapperToPolygonRoiConverter);

		poly.removeSplineFit();
		final Converter<?, ?> pWidth = convertService.getHandler(wrap,
			PolygonRoi.class);
		assertTrue(pWidth instanceof WrapperToPolygonRoiConverter);
	}

	@Test
	public void testUnwrapFreeLineWithWidth() {
		free.updateWideLine(2.5f);
		final PolygonRoi pr = convertService.convert(freeWrap, PolygonRoi.class);

		assertTrue(free == pr);
		assertEquals(Roi.FREELINE, pr.getType());
		assertEquals(2.5, pr.getStrokeWidth(), 0);
	}

	@Test
	public void testUnwrapPolylineWithWidth() {
		poly.updateWideLine(3);
		final PolygonRoi pr = convertService.convert(wrap, PolygonRoi.class);

		assertTrue(poly == pr);
		assertEquals(Roi.POLYLINE, pr.getType());
		assertEquals(3, pr.getStrokeWidth(), 0);
	}

	@Test
	public void testUnwrapSplineFitPolyline() {
		poly.fitSpline();
		final PolygonRoi pr = convertService.convert(wrap, PolygonRoi.class);

		assertTrue(poly == pr);
		assertEquals(Roi.POLYLINE, pr.getType());
		assertTrue(poly.isSplineFit());
	}

	@Test
	public void testUnwrapSplineFitPolylineWithWidth() {
		poly.updateWideLine(0.25f);
		poly.fitSpline();
		final PolygonRoi pr = convertService.convert(wrap, PolygonRoi.class);

		assertTrue(poly == pr);
		assertEquals(Roi.POLYLINE, pr.getType());
		assertEquals(0.25, pr.getStrokeWidth(), 0);
		assertTrue(pr.isSplineFit());
	}
}
