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

import ij.IJ;
import ij.WindowManager;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assume.assumeFalse;

/**
 * Test if a {@link Command} that returns a {@link Dataset} behaves as expected
 * when an ImageJ macro is run in batch mode.
 *
 * @author Matthias Arzt
 */
public class BatchModeTest {

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
		if(context != null)
			context.dispose();
	}

	/** Just for comparison, see how the original ImageJ behaves in batch mode. */
	@Test
	public void testOriginalImageJ() {
		WindowManager.closeAllWindows();
		IJ.runMacro(
			"setBatchMode(true);\n" +
			"newImage(\"imageA\", \"8-bit black\", 2, 2, 1);\n" +
			"newImage(\"imageB\", \"8-bit black\", 2, 2, 1);\n" +
			"run(\"Record Image Titles\");" +
			"setBatchMode(false);"
		);
		assertArrayEquals(new String[] {"imageA", "imageB"}, RecordImageTitles.titles);
	}

	@Test
	public void testBatchMode() {
		showLegacyUI();

		WindowManager.closeAllWindows();
		IJ.runMacro(
			"setBatchMode(true);" +
			"run(\"Output Dataset\", \"title=imageA\");" +
			"run(\"Output Dataset\", \"title=imageB\");" +
			"run(\"Record Image Titles\");" +
			"setBatchMode(false);"
		);
		assertArrayEquals(new String[] {"imageA", "imageB"}, RecordImageTitles.titles);
	}

	private void showLegacyUI() {
		// NB: The test fails, when the legacy ui is not visible.
		// TODO: This should be improved. The test should also work in headless mode, without the legacy ui visible.
		assumeFalse(GraphicsEnvironment.isHeadless());
		UIService service = context.service(UIService.class);
		service.showUI();
	}

	/** Simple command that outputs a {@link Dataset}. Used for testing. */
	@Plugin(type = Command.class, menuPath = "ImageJ Legacy Test > Output Dataset")
	public static class OutputDataset implements Command {

		@Parameter
		private DatasetService datasetService;

		@Parameter(required = false)
		public String title = "title";

		@Parameter(type = ItemIO.OUTPUT)
		public Dataset output;

		@Override
		public void run() {
			output = datasetService
				.create(new UnsignedByteType(), new long[] { 2, 2 }, title,
					new AxisType[] { Axes.X, Axes.Y });
		}
	}

	/** Command the stores all image titles in a static field. Used for testing. */
	@Plugin(type = Command.class, menuPath = "ImageJ Legacy Test > Record Image Titles")
	public static class RecordImageTitles implements Command {

		private static String[] titles;

		@Override
		public void run() {
			titles = WindowManager.getImageTitles();
		}
	}
}
