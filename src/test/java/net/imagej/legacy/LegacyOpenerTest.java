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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.net.URL;

import net.imagej.patcher.LegacyInjector;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * Verifies that opening image works as expected.
 * 
 * @author Johannes Schindelin
 */
public class LegacyOpenerTest {

	static {
		LegacyInjector.preinit();
	}

	/**
	 * This regression test is based on a macro provided by Paul van Schayck.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPaulsMacro() throws Exception {
		final URL url = getClass().getResource("/icons/imagej-256.png");
		assertNotNull(url);
		assumeTrue("file".equals(url.getProtocol()));
		final String path = url.getPath();
		final String macro = "// @OUTPUT int nResults\n"
				+ "open('" + path + "');\n"
				+ "if (nImages() != 1) exit('Oh no!');\n"
				+ "run('8-bit');"
				+ "setThreshold(150,255);"
				+ "run('Analyze Particles...', 'size=20-Infinity circularity=0.40-1.00');\n";

		final Context context = new Context();
		final ScriptService script = context.getService(ScriptService.class);
		assertNotNull(script);
		final ScriptModule module = script.run("pauls-macro.ijm", macro, true).get();
		final Integer nResults = (Integer) module.getOutput("nResults");
		assertNotNull(nResults);
		assertEquals(10, (int) nResults);
		context.dispose();
	}

	/**
	 * This regression test is based on a bug report by Bob Dougherty.
	 * <p>
	 * The original bug report can be found <a
	 * href="http://thread.gmane.org/gmane.comp.java.imagej/33423/focus=33443"
	 * >here</a>.
	 * </p>
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSliceLabels() throws Exception {
		final URL url = getClass().getResource("/with-slice-label.tif");
		assertNotNull(url);
		assumeTrue("file".equals(url.getProtocol()));

		final String path = url.getPath();
		final String macro = "// @OUTPUT String label\n"
				+ "open('" + path + "');\n"
				+ "if (nImages() != 1) exit('Oh no!');\n"
				+ "label = getMetadata('Label');\n";

		final Context context = new Context();
		final ScriptService script = context.getService(ScriptService.class);
		assertNotNull(script);
		final ScriptModule module = script.run("bobs-macro.ijm", macro, true).get();
		final String label = (String) module.getOutput("label");
		assertNotNull(label);
		assertEquals("Hello, World!", label);
		context.dispose();
	}
}
