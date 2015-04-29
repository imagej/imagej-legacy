/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
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

import java.net.URL;

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
import net.imagej.legacy.display.AbstractImagePlusDisplayViewer;
import net.imagej.legacy.display.LegacyImageDisplayService;
import net.imagej.legacy.display.LegacyImageDisplayViewer;
import net.imagej.legacy.plugin.ActiveImagePlusPreprocessor;
import net.imagej.legacy.plugin.DefaultLegacyOpener;
import net.imagej.legacy.plugin.IJ1MacroEngine;
import net.imagej.legacy.plugin.LegacyCommand;
import net.imagej.legacy.plugin.LegacyInitializer;
import net.imagej.legacy.translate.AbstractDisplayCreator;
import net.imagej.legacy.translate.AbstractImagePlusCreator;
import net.imagej.legacy.translate.ColorDisplayCreator;
import net.imagej.legacy.translate.ColorImagePlusCreator;
import net.imagej.legacy.translate.ColorPixelHarmonizer;
import net.imagej.legacy.translate.ColorTableHarmonizer;
import net.imagej.legacy.translate.CompositeHarmonizer;
import net.imagej.legacy.translate.DatasetImagePlusConverter;
import net.imagej.legacy.translate.DefaultImageTranslator;
import net.imagej.legacy.translate.GrayDisplayCreator;
import net.imagej.legacy.translate.GrayImagePlusCreator;
import net.imagej.legacy.translate.GrayPixelHarmonizer;
import net.imagej.legacy.translate.Harmonizer;
import net.imagej.legacy.translate.ImagePlusDatasetConverter;
import net.imagej.legacy.translate.LegacyUtils;
import net.imagej.legacy.translate.MergedRgbVirtualStack;
import net.imagej.legacy.translate.MetadataHarmonizer;
import net.imagej.legacy.translate.NameHarmonizer;
import net.imagej.legacy.translate.OverlayHarmonizer;
import net.imagej.legacy.translate.PlaneHarmonizer;
import net.imagej.legacy.translate.PositionHarmonizer;
import net.imagej.legacy.translate.ResultsTableHarmonizer;
import net.imagej.patcher.LegacyInjector;

import org.junit.Test;
import org.scijava.util.ClassUtils;
import org.scijava.util.FileUtils;

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

		final URL directory = ClassUtils.getLocation(IJ1Helper.class);
		final int prefixLength = directory.toString().length();
		for (final URL url : FileUtils.listContents(directory)) {
			final String path = url.toString().substring(prefixLength);
			if (!path.endsWith(".class")) continue;
			final String className = path.substring(0, path.length() - 6).replace('/', '.');

			if (className.startsWith(IJ1Helper.class.getName()) ||
					/* TODO: At least some of them should not need to access ImageJ 1.x classes directly! */
					className.startsWith(LegacyOutputTracker.class.getName()) ||
					className.startsWith(LegacyCommand.class.getName()) ||
					className.startsWith(ActiveImagePlusPreprocessor.class.getName()) ||
					className.startsWith(DefaultLegacyOpener.class.getName()) ||
					className.startsWith(IJ1MacroEngine.class.getName()) ||
					className.startsWith(LegacyInitializer.class.getName()) ||
					className.startsWith(SwitchToModernMode.class.getName()) ||
					className.startsWith(DefaultLegacyHooks.class.getName()) ||
					className.startsWith(LegacyImageMap.class.getName()) ||
					className.startsWith(PlaneHarmonizer.class.getName()) ||
					className.startsWith(AbstractImagePlusCreator.class.getName()) ||
					className.startsWith(ImagePlusDatasetConverter.class.getName()) ||
					className.startsWith(GrayPixelHarmonizer.class.getName()) ||
					className.startsWith(ColorDisplayCreator.class.getName()) ||
					className.startsWith(DefaultImageTranslator.class.getName()) ||
					className.startsWith(NameHarmonizer.class.getName()) ||
					className.startsWith(ColorTableHarmonizer.class.getName()) ||
					className.startsWith(Harmonizer.class.getName()) ||
					className.startsWith(OverlayHarmonizer.class.getName()) ||
					className.startsWith(MergedRgbVirtualStack.class.getName()) ||
					className.startsWith(ColorPixelHarmonizer.class.getName()) ||
					className.startsWith(MetadataHarmonizer.class.getName()) ||
					className.startsWith(GrayDisplayCreator.class.getName()) ||
					className.startsWith(ColorImagePlusCreator.class.getName()) ||
					className.startsWith(GrayImagePlusCreator.class.getName()) ||
					className.startsWith(PositionHarmonizer.class.getName()) ||
					className.startsWith(ResultsTableHarmonizer.class.getName()) ||
					className.startsWith(LegacyUtils.class.getName()) ||
					className.startsWith(AbstractDisplayCreator.class.getName()) ||
					className.startsWith(CompositeHarmonizer.class.getName()) ||
					className.startsWith(DatasetImagePlusConverter.class.getName()) ||
					className.startsWith(OptionsSynchronizer.class.getName()) ||
					className.startsWith(LegacyImageDisplayViewer.class.getName()) ||
					className.startsWith(LegacyImageDisplayService.class.getName()) ||
					className.startsWith(AbstractImagePlusDisplayViewer.class.getName())) {
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
