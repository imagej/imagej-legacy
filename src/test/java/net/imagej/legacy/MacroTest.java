/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2014 Board of Regents of the University of
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.scijava.test.TestUtils.createTemporaryDirectory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import net.imagej.patcher.LegacyInjector;

import org.junit.Test;
import org.scijava.Context;

/**
 * Verifies that ImageJ 1.x macros work as expected by the user.
 * 
 * @author Johannes Schindelin
 */
public class MacroTest {

	static {
			LegacyInjector.preinit();
	}

	@Test
	public void testLegacyCompatibleMacro() throws Exception {
		final File tmp = createTemporaryDirectory("macro-");
		final String imagejDirKey = "imagej.dir";
		final String imagejDir = System.getProperty(imagejDirKey);
		try {
			System.setProperty(imagejDirKey, tmp.getPath());
			final File plugins = new File(tmp, "plugins");
			final File scripts = new File(plugins, "Scripts");
			final File analyze = new File(scripts, "Analyze");
			assertTrue(analyze.mkdirs());

			final File addArguments = new File(analyze, "Add_Arguments.ijm");
			final FileWriter writer = new FileWriter(addArguments);
			writer.write("a = getNumber('a', 0); b = getNumber('b', 0);\n" +
				"result = a + b;\n" +
				"path = call('java.lang.System.getProperty', 'imagej.dir') + '/result.txt';\n" +
				"File.append('The result is ' + result, path);\n");
			writer.close();

			final Context context = new Context();
			// Prevent the test class from loading the ij.IJ class
			new Runnable() {
				@Override
				public void run() {
					ij.IJ.run("Add Arguments", "a=17 b=37");
				}
			}.run();

			final File result = new File(tmp, "result.txt");
			assertTrue(result.exists());
			assertEquals("The result is 54\n", readFile(result));

			context.dispose();
		}
		finally {
			if (imagejDir == null) System.clearProperty(imagejDirKey);
			else System.setProperty(imagejDirKey, imagejDir);
		}
	}

	private String readFile(final File file) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length());
		final FileInputStream in = new FileInputStream(file);
		final byte[] buffer = new byte[16384];
		for (;;) {
			int count = in.read(buffer);
			if (count < 0) break;
			out.write(buffer, 0, count);
		}
		in.close();
		out.close();
		return new String(out.toString("UTF-8"));
	}
}
