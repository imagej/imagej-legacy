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
			try (final FileWriter writer = new FileWriter(addArguments)) {
				writer.write("a = getNumber('a', 0); b = getNumber('b', 0);\n" +
					"result = a + b;\n" +
					"path = call('java.lang.System.getProperty', 'imagej.dir') + '/result.txt';\n" +
					"File.append('The result is ' + result, path);\n");
			}

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

	/**
	 * Tests that bare .java plugins can see the correct macro options.
	 * <p>
	 * This is essentially a shrunk-down version of the problem described
	 * by Aryeh Weiss in
	 * http://fiji.sc/bugzilla/show_bug.cgi?id=787
	 * </p>
	 */
	@Test
	public void testBarePluginFromMacro() throws Exception {
		final String imagejDirKey = "imagej.dir";
		final String imagejDir = System.getProperty(imagejDirKey);

		final File tmp = createTemporaryDirectory("bare-");
		final File barePlugin = new File(tmp, "plugins/Set_Property_Test.java");
		assertTrue(barePlugin.getParentFile().mkdirs());
		writeFile(barePlugin,
			"import ij.IJ;",
			"import ij.gui.GenericDialog;",
			"import ij.plugin.PlugIn;",
			"",
			"public class Set_Property_Test implements PlugIn {",
			"  public void run(final String arg) {",
			"    final GenericDialog gd = new GenericDialog(\"Hello, World!\");",
			"    gd.addStringField(\"dir\", \"\");",
			"    gd.showDialog();",
			"    if (gd.wasCanceled()) return;",
			"    System.setProperty(\"" + imagejDirKey + "\", gd.getNextString());",
			"  }",
			"}");
		try {
			System.setProperty(imagejDirKey, tmp.getPath());
			final Context context = new Context();
			// Prevent the test class from loading the ij.IJ class
			new Runnable() {
				@Override
				public void run() {
					ij.IJ.run("Set Property Test", "dir=c:\\hello\\world");
				}
			}.run();
			context.dispose();
			assertEquals("c:\\hello\\world", System.getProperty(imagejDirKey));
		}
		finally {
			if (imagejDir == null) System.clearProperty(imagejDirKey);
			else System.setProperty(imagejDirKey, imagejDir);
		}
	}

	private String readFile(final File file) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length());
		try (final FileInputStream in = new FileInputStream(file)) {
			final byte[] buffer = new byte[16384];
			for (;;) {
				int count = in.read(buffer);
				if (count < 0) break;
				out.write(buffer, 0, count);
			}
		}
		return new String(out.toString("UTF-8"));
	}

	private void writeFile(final File file, final String... lines) throws IOException {
		try (final FileWriter writer = new FileWriter(file)) {
			for (final String line : lines) {
				writer.write(line);
				writer.write("\n");
			}
		}
	}
}
