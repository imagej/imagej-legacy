/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2020 ImageJ developers.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.object.ObjectService;
import org.scijava.script.ScriptLanguage;
import org.scijava.script.ScriptModule;
import org.scijava.script.ScriptService;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * Tests whether {@link IJ1MacroLanguage} works conveniently with named Objects present in the {@link ObjectService}
 *
 * This test works in combination with {@link MacroRecorderPostprocessorNamedObjectTest}
 *
 * Extra : groovy : should return the Object while in IJ1
 *
 * TODO : how to deal with duplicate names ? In terms of testing ?
 *
 * @author Nicolas Chiaruttini
 */

public class IJ1MacroLanguageNamedObjectTest {

	private Context context;
	private ScriptService scriptService;
	private ObjectService objectService;

	@Before
	public void setUp() {
		context = new Context();
		scriptService = context.service(ScriptService.class);
		objectService = context.service(ObjectService.class);
	}

	@After
	public void tearDown() {
		context.dispose();
	}

	@Test
	public void testGroovyNamedObject() throws InterruptedException,
			ExecutionException
	{
		// TODO : Do we want this behaviour with groovy ? But do we have a choice ?
		// Puts a named Pet object into the ObjectService
		Pet pet = new Pet("Felix", "Cat");
		objectService.addObject(pet,pet.getName());

		// Makes Pet class easily discoverable be the script service
		scriptService.addAlias(Pet.class);

		// Execute a script.
		final String script = "" + //
				"#@ Pet animal\n" + //
				"#@ ObjectService objectService\n" + //
				"#@output String greeting\n" + //
				"greeting = 'Oh, '+objectService.getName(animal)+' is so cute!'\n" + //
				"";

		final ScriptModule m = scriptService.run("greet.groovy", script, true, //
				"animal", "Felix").get(); // Felix is given -> needs String to Pet conversion

		final Object returnValue = m.getReturnValue();

		// Verify that the input has been found - felix the cat
		// Correct class
		assertSame(m.getInput("animal").getClass(), Pet.class);
		// Correct instance
		assertSame(m.getInput("animal"), pet);

		// Verify that the script executed correctly - with the correct object being
		// given to the script (groovy behaviour)
		assertEquals("Oh, Felix is so cute!", //
				m.getOutput("greeting"));

	}


	@Test
	public void testIJ1MacroLanguageNamedObject() throws InterruptedException,
			ExecutionException
	{
		// Puts a named Pet object into the ObjectService
		Pet pet = new Pet("Felix", "Cat");
		objectService.addObject(pet,pet.getName());

		// Makes Pet class easily discoverable be the script service
		scriptService.addAlias(Pet.class);

		// Execute a script.
		final String script = "" + //
				"#@ Pet animal\n" + //
				"#@output String greeting\n" + //
				"greeting = 'Oh, '+animal+' is so cute!'\n" + //
				"";

		final ScriptModule m = scriptService.run("greet.ijm", script, true, //
				"animal", pet).get();

		final Object returnValue = m.getReturnValue();

		// Verify that the input has been found - felix the cat
		// Correct class
		assertSame(m.getInput("animal").getClass(), Pet.class);
		// Correct instance
		assertSame(m.getInput("animal"), pet);

		// Verify that the script executed correctly - with the correct object NAME being
		// given to the script (IJ1 Macro Language behaviour)
		assertEquals("Oh, Felix is so cute!", //
				m.getOutput("greeting"));

	}

	/*
		Simple "custom" object
	 */
	static class Pet {

		private String name;

		Pet(String name, String species) {
			this.name = name;
		}

		// Unfortunately not a toString overriden method
		public String getName() {
			return name;
		}

	}

}
