/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2016 Board of Regents of the University of
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

package net.imagej.legacy.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.script.ScriptException;

import org.junit.Test;
import org.scijava.Context;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

/**
 * Tests {@link IJ1MacroLanguage}.
 * 
 * @author Curtis Rueden
 */
public class IJ1MacroLanguageTest {

	@Test
	public void testReturnValues() throws InterruptedException, ExecutionException,
		IOException, ScriptException
	{
		final Context context = new Context();
		final ScriptService scriptService = context.service(ScriptService.class);
		final String script = "return \"green eggs and ham\"";
		final ScriptModule m = scriptService.run("return.ijm", script, true).get();
		final Object returnValue = m.getReturnValue();
		assertSame(String.class, returnValue.getClass());
		assertEquals("green eggs and ham", returnValue);

		context.dispose();
	}

	@Test
	public void testParameters() throws InterruptedException, ExecutionException,
		IOException, ScriptException
	{
		final Context context = new Context();
		final ScriptService scriptService = context.service(ScriptService.class);

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

		context.dispose();
	}
}
