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

package net.imagej.legacy;

import java.net.URL;

import net.imagej.patcher.LegacyInjector;

import org.junit.Test;
import org.scijava.util.FileUtils;
import org.scijava.util.Types;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.Cast;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.Handler;
import javassist.expr.Instanceof;
import javassist.expr.MethodCall;
import javassist.expr.NewArray;
import javassist.expr.NewExpr;

/**
 * Verifies that ImageJ 1.x classes are used only via the {@link IJ1Helper} class.
 * 
 * @author Johannes Schindelin
 */
public class ImageJ1EncapsulationTest {

	static {
		try {
			LegacyInjector.preinit();
		}
		catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException("Got exception (see error log)");
		}
	}

	@Test
	public void verifyEncapsulation() throws Exception {
		final ClassPool pool = ClassPool.getDefault();

		final URL directory = Types.location(IJ1Helper.class);
		final int prefixLength = directory.toString().length();
		for (final URL url : FileUtils.listContents(directory)) {
			final String path = url.toString().substring(prefixLength);
			if (!path.endsWith(".class")) continue;
			final String className = path.substring(0, path.length() - 6).replace('/', '.');

			if (className.startsWith(IJ1Helper.class.getName()) ||
					/* TODO: At least some of them should not need to access ImageJ 1.x classes directly! */
					className.startsWith(net.imagej.legacy.DefaultLegacyHooks.class.getName()) ||
					className.startsWith(net.imagej.legacy.LegacyImageMap.class.getName()) ||
					className.startsWith(net.imagej.legacy.Macros.class.getName()) ||
					className.startsWith(net.imagej.legacy.OptionsSynchronizer.class.getName()) ||
					className.startsWith(net.imagej.legacy.command.LegacyCommand.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.AbstractImagePlusLegacyConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.DatasetToImagePlusConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.DoubleToImagePlusConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ImageDisplayToImagePlusConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ImagePlusToDatasetConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ImagePlusToImageDisplayConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ImagePlusToImgPlusConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ImageTitleToImagePlusConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ImgPlusToImagePlusConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.OverlayToROITreeConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ResultsTableColumnWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ResultsTableToGenericTableConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ResultsTableUnwrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ResultsTableWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.ROITreeToOverlayConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.StringToImagePlusConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.StringToDatasetConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.TableListWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.TableToResultsTableConverters.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.TableUnwrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.TableWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.AbstractMaskPredicateToRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.AbstractPolygonRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.AbstractRoiToMaskPredicateConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.AbstractRoiUnwrapConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.BinaryCompositeMaskPredicateToShapeRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.DefaultRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.IJRealRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.IJRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.RealMaskRealIntervalToImageRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.RoiToMaskIntervalConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.RoiUnwrappers.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.ShapeRoiToMaskRealIntervalConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.ShapeRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.box.BoxToRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.box.BoxWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.box.RoiToBoxConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.box.RoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.box.WritableBoxToRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.ellipsoid.EllipsoidToOvalRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.ellipsoid.EllipsoidWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.ellipsoid.OvalRoiToEllipsoidConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.ellipsoid.OvalRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.ellipsoid.WritableEllipsoidToOvalRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.line.IJLineToLineConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.line.IJLineWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.line.LineToIJLineConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.line.LineWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.line.WritableLineToIJLineConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.point.PointMaskToPointRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.point.PointMaskWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.point.PointRoiToRealPointCollectionConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.point.PointRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.point.RealPointCollectionToPointRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.point.RealPointCollectionWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.point.WritablePointMaskToPointRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.point.WritableRealPointCollectionToPointRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polygon2d.Polygon2DToPolygonRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polygon2d.Polygon2DWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polygon2d.PolygonRoiToPolygon2DConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polygon2d.PolygonRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polygon2d.UnmodifiablePolygonRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polygon2d.WritablePolygon2DToPolygonRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polyline.IrregularPolylineRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polyline.PolylineRoiToPolylineConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polyline.PolylineRoiToRealMaskRealIntervalConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polyline.PolylineRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polyline.PolylineToPolylineRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polyline.PolylineWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polyline.UnmodifiablePolylineRoiWrapper.class.getName()) ||
					className.startsWith(net.imagej.legacy.convert.roi.polyline.WritablePolylineToPolylineRoiConverter.class.getName()) ||
					className.startsWith(net.imagej.legacy.display.ImagePlusDisplayViewer.class.getName()) ||
					className.startsWith(net.imagej.legacy.display.LegacyImageDisplayService.class.getName()) ||
					className.startsWith(net.imagej.legacy.display.LegacyImageDisplayViewer.class.getName()) ||
					className.startsWith(net.imagej.legacy.plugin.ActiveImagePlusPreprocessor.class.getName()) ||
					className.startsWith(net.imagej.legacy.plugin.DefaultLegacyOpener.class.getName()) ||
					className.startsWith(net.imagej.legacy.plugin.IJ1MacroEngine.class.getName()) ||
					className.startsWith(net.imagej.legacy.plugin.LegacyInitializer.class.getName()) ||
					className.startsWith(net.imagej.legacy.plugin.OverlayPreprocessor.class.getName()) ||
					className.startsWith(net.imagej.legacy.plugin.ResultsTablePreprocessor.class.getName()) ||
					className.startsWith(net.imagej.legacy.plugin.RoiManagerPreprocessor.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.ColorTableHarmonizer.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.CompositeHarmonizer.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.DisplayCreator.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.Harmonizer.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.ImagePlusCreator.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.ImagePlusCreatorUtils.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.ImageTranslator.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.LegacyUtils.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.MetadataHarmonizer.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.NameHarmonizer.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.OverlayHarmonizer.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.PositionHarmonizer.class.getName()) ||
					className.startsWith(net.imagej.legacy.translate.ResultsTableHarmonizer.class.getName()))
			{
				continue;
			}
			try {
				final CtClass clazz = pool.get(className);
				clazz.instrument(new ImageJ1UsageTester());
			} catch (final Exception e) {
				throw new RuntimeException("Problem with class " + className, e);
			}
		}
	}

	private final class ImageJ1UsageTester extends ExprEditor {

		private void test(final CtClass c) {
			if (c != null && c.getName().startsWith("ij.")) {
				throw new RuntimeException("ImageJ 1.x class used: " + c.getName());
			}
		}

		@Override
		public void edit(Cast c) {
			try {
				test(c.getType());
			}
			catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void edit(ConstructorCall c) {
			try {
				test(c.getConstructor().getDeclaringClass());
				final CtConstructor c2 = c.getConstructor();
				for (final CtClass c3 : c2.getExceptionTypes()) {
					test(c3);
				}
				for (final CtClass c3 : c2.getParameterTypes()) {
					test(c3);
				}
			}
			catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void edit(FieldAccess f) {
			try {
				final CtField field = f.getField();
				test(field.getDeclaringClass());
				test(field.getType());
			}
			catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void edit(Handler h) {
			try {
				test(h.getType());
			}
			catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void edit(Instanceof i) {
			try {
				test(i.getType());
			}
			catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void edit(MethodCall m) {
			try {
				final CtMethod m2 = m.getMethod();
				test(m2.getDeclaringClass());
				test(m2.getReturnType());
				for (final CtClass c2 : m2.getExceptionTypes()) {
					test(c2);
				}
				for (final CtClass c2 : m2.getParameterTypes()) {
					test(c2);
				}
			}
			catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void edit(NewArray a) {
			try {
				test(a.getComponentType());
			}
			catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void edit(NewExpr e) {
			try {
				final CtConstructor c = e.getConstructor();
				for (final CtClass c2 : c.getExceptionTypes()) {
					test(c2);
				}
				for (final CtClass c2 : c.getParameterTypes()) {
					test(c2);
				}
			}
			catch (NotFoundException e2) {
				throw new RuntimeException(e2);
			}
		}
	}
}
