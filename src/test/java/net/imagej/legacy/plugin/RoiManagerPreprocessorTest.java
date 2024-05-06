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

package net.imagej.legacy.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * Tests {@link RoiManagerPreprocessor}.
 *
 * @author Jan Eglinger
 */
public class RoiManagerPreprocessorTest {

	private Context context;
	private ScriptService scriptService;

	@Before
	public void setUp() {
		context = new Context();
		scriptService = context.service(ScriptService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testRoiManagerParameter() throws InterruptedException,
			ExecutionException {
		assumeTrue(!GraphicsEnvironment.isHeadless());
		RoiManager roiManager = new RoiManager();
		roiManager.addRoi(new Roi(0, 0, 100, 100));
		final String script = "" + //
				"// @RoiManager rm\n" + //
				"// @OUTPUT int number\n" + //
				"number = rm.getCount()\n" + //
				"";
		final ScriptModule m = scriptService.run("roiManager.groovy", script,
				true).get();
		assertEquals(1, m.getOutput("number"));
	}
}
