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

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.run.AbstractCodeRunner;
import org.scijava.run.CodeRunner;
import org.scijava.util.Types;

/**
 * Runs the given ImageJ 1.x {@code PlugIn} class.
 * 
 * @author Curtis Rueden
 */
@Plugin(type = CodeRunner.class)
public class LegacyCodeRunner extends AbstractCodeRunner {

	@Parameter
	private LegacyService legacyService;

	// -- CodeRunner methods --

	@Override
	public void run(final Object code, final Object... args)
		throws InvocationTargetException
	{
		final Class<?> c = getPlugInClass(code);
		if (code != null) IJ1Helper.run(c);
		// TODO: Pass args somehow.
		// * arg -- PlugIn.run(String) and IJ.runPlugIn(String, String)
		// * options -- IJ.run(String, String)
	}

	@Override
	public void run(final Object code, final Map<String, Object> inputMap)
		throws InvocationTargetException
	{
		final Class<?> c = getPlugInClass(code);
		if (code != null) IJ1Helper.run(c);
		// TODO: Pass args somehow.
		// * arg -- PlugIn.run(String) and IJ.runPlugIn(String, String)
		// * options -- IJ.run(String, String)
	}

	// -- Typed methods --

	@Override
	public boolean supports(final Object code) {
		return getPlugInClass(code) != null;
	}

	// -- Helper methods --

	private Class<?> getPlugInClass(final Object code) {
		final Class<?> c = asClass(code);
		if (c == null) return null;
		// NB: No direct refs to ImageJ 1.x API, to avoid class loading issues.
		if (c.getName().equals("ij.plugin.PlugIn")) return c;
		if (c.getName().equals("ij.plugin.filter.PlugInFilter")) return c;
		if (getPlugInClass(c.getSuperclass()) != null) return c;
		for (final Class<?> iface : c.getInterfaces()) {
			if (getPlugInClass(iface) != null) return c;
		}
		return null;
	}

	// TODO: Migrate this common function to AbstractCodeRunner?
	// Or... make it a Converter plugin?
	private Class<?> asClass(final Object code) {
		if (code instanceof Class) return (Class<?>) code;
		if (code instanceof String) return Types.load((String) code);
		return null;
	}

}
