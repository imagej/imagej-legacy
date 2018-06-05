/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2018 Board of Regents of the University of
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
import ij.IJ;
import ij.ImagePlus;

import ij.io.FileInfo;
import ij.io.OpenDialog;

import ij.plugin.PlugIn;

import ij.process.ByteProcessor;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This is a template for an I/O plugin.
 *
 * Since plugins do not have return values, the only way to let
 * ImageJ handle the display of the resulting window (or not, if
 * we're in batch mode) is to extend ImagePlus.
 *
 * This is only relevant if the class HandleExtraFileTypes is extended
 * to fall back to this reader.
 */
public class Bare_Reader_PlugIn extends ImagePlus implements PlugIn {
	/**
	 * This method gets called by ImageJ / Fiji.
	 *
	 * @param path is expected to be a path if it is not empty
	 * @see ij.plugin.PlugIn#run(java.lang.String)
	 */
	@Override
	public void run(String path) {
		boolean needToShow = false;

		// get the file
		File file;
		if (path == null || path.equals("")) {
			OpenDialog dialog = new OpenDialog("Open .serialized file", null);
			if (dialog.getDirectory() == null)
				return; // canceled
			file = new File(dialog.getDirectory(), dialog.getFileName());

			/*
			 * Since no path was passed, assume that it was run interactively
			 * rather than from HandleExtraFileTypes
			 */
			needToShow = true;
		}
		else
			file = new File(path);

		// read the file (in this example, a 256x256 8-bit grayscale raw image)
		int width = 256, height = 256;
		byte[] pixels = new byte[width * height];
		try {
			DataInputStream input = new DataInputStream(new FileInputStream(file));
			input.readFully(pixels);
		} catch (IOException e) {
			IJ.error("Could not read " + file.getAbsolutePath() + ":\n" + e.getMessage());
			return;
		}
		ByteProcessor processor = new ByteProcessor(width, height, pixels, null);

		// now set the contents of the ImagePlus
		setProcessor(processor);
		setTitle(file.getName());

		setProperty("Info", "This text is shown in Image>Show Info...");

		// setting the FileInfo is optional
		FileInfo info = new FileInfo();
		info.fileName = file.getAbsolutePath();
		info.width = width;
		info.height = height;
		info.nImages = 1;
		info.fileFormat = FileInfo.RAW;
		setFileInfo(info);

		if (needToShow)
			show();
	}
}
