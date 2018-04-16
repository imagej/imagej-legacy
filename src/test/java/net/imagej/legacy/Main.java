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

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.legacy.ui.LegacyUI;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.planar.PlanarImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Launches ImageJ with the legacy UI.
 * 
 * @author Curtis Rueden
 */
public class Main {

	public static void main(String[] args) {
		final Context context = new Context();
		context.service(UIService.class).showUI(LegacyUI.NAME);
	}

	@Plugin(type = Command.class, menuPath = "Test > Create ArrayImg")
	public static class CreateArrayImg implements Command {

		@Parameter(type = ItemIO.OUTPUT)
		private Dataset result;

		@Parameter
		private DatasetService datasetService;

		@Override
		public void run() {
			AxisType[] axisTypes = { Axes.X, Axes.Y, Axes.Z };
			result = datasetService.create(new ImgPlus<>(ArrayImgs.unsignedBytes(100, 100, 100), "array img", axisTypes));
			Cursor<RealType<?>> cursor = result.localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				double x = cursor.getDoublePosition(0);
				cursor.get().setReal(Math.sin(x / 20) * 127 + 127);
			}
		}
	}

	@Plugin(type = Command.class, menuPath = "Test > Create ArrayImg BitType")
	public static class CreateBitTypeImg implements Command {

		@Parameter(type = ItemIO.OUTPUT)
		private Dataset result;

		@Parameter
		private DatasetService datasetService;

		@Override
		public void run() {
			AxisType[] axisTypes = { Axes.X, Axes.Y, Axes.Z };
			result = datasetService.create(new ImgPlus<>(ArrayImgs.bits(100, 100, 100), "array img", axisTypes));
			Cursor<RealType<?>> cursor = result.localizingCursor();
			while (cursor.hasNext()) {
				cursor.fwd();
				double x = cursor.getDoublePosition(0);
				double y = cursor.getDoublePosition(1);
				cursor.get().setReal(Math.sin(y / 10) * Math.sin(x / 10) * 127 + 127);
			}
		}
	}

	@Plugin(type = Command.class, menuPath = "Test > Create PlanarImg")
	public static class CreatePlanerImg implements Command {

		@Parameter(type = ItemIO.OUTPUT)
		private Dataset result;

		@Parameter
		private DatasetService datasetService;

		@Override
		public void run() {
			AxisType[] axisTypes = { Axes.X, Axes.Y, Axes.Z };
			result = datasetService.create(new ImgPlus<>(PlanarImgs.unsignedBytes(100, 100, 100), "planer img", axisTypes));
		}
	}

	@Plugin(type = Command.class, menuPath = "Test > Create 2d ArrayImg")
	public static class Create2dArrayImg implements Command {

		@Parameter(type = ItemIO.OUTPUT)
		private Dataset result;

		@Parameter
		private DatasetService datasetService;

		@Override
		public void run() {
			AxisType[] axisTypes = { Axes.X, Axes.Y };
			result = datasetService.create(new ImgPlus<>(ArrayImgs.unsignedBytes(100, 100), "2d array img", axisTypes));
		}
	}
}
