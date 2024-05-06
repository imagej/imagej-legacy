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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.ExecutionException;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * Tests {@link IJ1MacroLanguage}.
 * 
 * @author Curtis Rueden
 */
public class IJ1MacroLanguageTest {

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
	public void testReturnValues() throws InterruptedException,
		ExecutionException
	{
		final String script = "return \"green eggs and ham\"";
		final ScriptModule m = scriptService.run("return.ijm", script, true).get();
		final Object returnValue = m.getReturnValue();
		assertSame(String.class, returnValue.getClass());
		assertEquals("green eggs and ham", returnValue);
	}

	@Test
	public void testBindings() throws ScriptException {
		final ScriptLanguage language = scriptService.getLanguageByExtension("ijm");
		final ScriptEngine engine = language.getScriptEngine();
		assertSame(IJ1MacroEngine.class, engine.getClass());

		assertNull(engine.get("hello"));
		assertNull(engine.get("temperature"));
		assertNull(engine.get("salutations"));

		// populate bindings with initial values
		engine.put("hello", "salutations");
		engine.put("temperature", 98.6);
		engine.put("goodbye", "farewell");
		assertEquals("salutations", engine.get("hello"));
		assertEquals(98.6, engine.get("temperature"));
		assertEquals("farewell", engine.get("goodbye"));

		// execute a macro which changes those values
		final Object returnValue = engine.eval("" + //
			"hello = \"greetings and \" + hello\n" + //
			"temperature = 37" + //
			"return \"bye\"\n" + //
			"goodbye = \"adios\"" //
		);
		assertEquals("bye", returnValue);
		assertEquals("greetings and salutations", engine.get("hello"));
		assertEquals(37.0, engine.get("temperature"));
		assertEquals("farewell", engine.get("goodbye"));

		// clear the bindings
		final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		bindings.size();
		bindings.clear();
		assertNull(engine.get("hello"));
		assertNull(engine.get("goodbye"));
	}

	@Test
	public void testParameters() throws InterruptedException, ExecutionException {
		final String script = "" + //
			"// @String name\n" + //
			"// @int age\n" + //
			"// @OUTPUT String greeting\n" + //
			"greeting = \"Hello, \" + name + \"! Happy birthday #\" + age;\n";
		final ScriptModule m = scriptService.run("greeting.ijm", script, true, //
			"name", "Oliver Twist", //
			"age", 9 //
		).get();

		final Object actual = m.getOutput("greeting");
		assertEquals("Hello, Oliver Twist! Happy birthday #9", actual);
	}
}
