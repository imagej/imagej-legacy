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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPreprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * Tests {@link MacroRecorderPostprocessor}.
 *
 * @author Curtis Rueden
 */
public class MacroRecorderPostprocessorTest {

	private Context context;
	private ScriptService scriptService;
	private LegacyService legacyService;

	@Before
	public void setUp() {
		context = new Context();
		scriptService = context.service(ScriptService.class);
		legacyService = context.service(LegacyService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testParametersRecorded() throws InterruptedException,
		ExecutionException
	{
		// NB: Register a preprocessor that injects input values and resolves them.
		// We need this, rather than passing the key/value pairs directly to the
		// run method, because when you pass parameters directly to the module
		// execution, they are immediately marked as resolved. But for the purposes
		// of macro recording, we need the parameters to be _unresolved_ initially,
		// and only later resolved around the time that input harvesting occurs.
		context.service(PluginService.class).addPlugin(new PluginInfo<>(
			MockInputHarvester.class, PreprocessorPlugin.class));

		context.service(PluginService.class).addPlugin(new PluginInfo<>(
				MacroRecorderPostprocessor.class, PostprocessorPlugin.class));

		// NB: Override the IJ1Helper to remember which parameters get recorded.
		final EideticIJ1Helper ij1Helper = new EideticIJ1Helper();
		legacyService.setIJ1Helper(ij1Helper);

		// Execute a script.
		final String script = "" + //
			"#@ String(style = 'password') password\n" + //
			"#@ String name\n" + //
			"#@ int bangCount\n" + //
			"#@ boolean(visibility = INVISIBLE) verbose\n" + //
			"#@output String greeting\n" + //
			"greeting = 'Hello, ' + name\n" + //
			"(1..bangCount).each { greeting += '!' }\n" + //
			"if (verbose) greeting += ' Nice to meet you!'\n" + //
			"";
		final ScriptModule m = //
			scriptService.run("greet.groovy", script, true).get();

		// Verify that the script executed correctly.
		assertEquals("Hello, Chuckles!!!!! Nice to meet you!", //
			m.getOutput("greeting"));

		// Verify that the parameters were recorded.
		assertEquals(2, ij1Helper.recordedArgs.size());
		assertEquals("name=Chuckles", ij1Helper.recordedArgs.get(0));
		assertEquals("bangCount=5", ij1Helper.recordedArgs.get(1));
	}

	// -- Helper classes --

	/**
	 * Extended version of {@link IJ1Helper} that remembers recorded parameters.
	 */
	private class EideticIJ1Helper extends IJ1Helper {

		public EideticIJ1Helper() {
			super(legacyService);
		}

		private final List<String> recordedArgs = new ArrayList<>();
		private boolean finished;

		@Override
		public void recordOption(final String key, final String value) {
			if (finished) {
				recordedArgs.clear();
				finished = false;
			}
			recordedArgs.add(key + "=" + value);
			super.recordOption(key, value);
		}

		@Override
		public void finishRecording() {
			super.finishRecording();
			finished = true;
		}
	}

	public static class MockInputHarvester extends AbstractPreprocessorPlugin {

		@Override
		public void process(final Module module) {
			module.setInput("password", "FooleryShinesEverywhere");
			module.resolveInput("password");
			module.setInput("name", "Chuckles");
			module.resolveInput("name");
			module.setInput("bangCount", 5);
			module.resolveInput("bangCount");
			module.setInput("verbose", true);
			module.resolveInput("verbose");
		}
	}
}
