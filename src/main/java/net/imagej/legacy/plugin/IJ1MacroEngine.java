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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.ScriptException;

import net.imagej.legacy.IJ1Helper;

import org.scijava.module.ModuleItem;
import org.scijava.script.AbstractScriptEngine;
import org.scijava.script.ScriptModule;

/**
 * An almost JSR-223-compliant script engine for the ImageJ 1.x macro language.
 * <p>
 * As far as possible, this script engine conforms to JSR-223. It lets the user
 * evaluate ImageJ 1.x macros. Due to the ImageJ 1.x macro interpreter's
 * limitations, functionality such as the {@link #get(String)} method is not
 * supported, however.
 * </p>
 *
 * @author Johannes Schindelin
 * @author Curtis Rueden
 */
public class IJ1MacroEngine extends AbstractScriptEngine {

	private final IJ1Helper ij1Helper;
	private ScriptModule module;

	private static ThreadLocal<Bindings> outputs = new ThreadLocal<>();

	public static void setOutput(final String key, final String value) {
		outputs.get().put(key, value);
	}

	/**
	 * Constructs an ImageJ 1.x macro engine.
	 *
	 * @param ij1Helper the helper to evaluate the macros
	 */
	public IJ1MacroEngine(final IJ1Helper ij1Helper) {
		this.ij1Helper = ij1Helper;
		engineScopeBindings = new IJ1MacroBindings();
	}

	@Override
	public Object eval(final String macro) throws ScriptException {
		// collect input variable key/value pairs from bindings + module inputs
		final LinkedHashMap<String, Object> inVars = new LinkedHashMap<>();
		inVars.putAll(engineScopeBindings);
		if (module != null) inVars.putAll(module.getInputs());

		final StringBuilder pre = new StringBuilder();

		// prepend variable assignments to the macro
		for (final Entry<String, Object> entry : inVars.entrySet()) {
			appendVar(pre, entry.getKey(), entry.getValue());
		}

		final StringBuilder post = new StringBuilder();
		if (module != null) {
			outputs.set(engineScopeBindings);
			for (final Entry<String, Object> entry : module.getOutputs().entrySet()) {
				post.append("call(\"");
				post.append(getClass().getName());
				post.append(".setOutput\", \"");
				post.append(entry.getKey()).append("\", ");
				post.append(entry.getKey()).append(");\n");
			}
		}

		final String returnValue = ij1Helper.runMacro(pre + macro + post);
		if (module != null) {
			// convert ImagePlus IDs to their corresponding instances
			for (final ModuleItem<?> item : module.getInfo().outputs()) {
				if (ij1Helper.isImagePlus(item.getType())) {
					final String name = item.getName();
					final Object value = convertToImagePlus(get(name));
					if (value != null) put(name, value);
				}
			}
			outputs.remove();
		}
		if ("[aborted]".equals(returnValue)) {
			// NB: Macro was canceled. Return null, to avoid displaying the output.
			return null;
		}
		return returnValue;
	}

	@Override
	public Object eval(final Reader reader) throws ScriptException {
		final StringBuilder builder = new StringBuilder();
		final char[] buffer = new char[16384];
		try {
			for (;;) {
				final int count = reader.read(buffer);
				if (count < 0) {
					break;
				}
				builder.append(buffer, 0, count);
			}
			reader.close();
		}
		catch (final IOException e) {
			throw new ScriptException(e);
		}
		return eval(builder.toString());
	}

	@Override
	public void put(final String key, final Object value) {
		if (ScriptModule.class.getName().equals(key)) {
			module = (ScriptModule) value;
			return;
		}
		engineScopeBindings.put(key, value);
	}

	// -- Helper methods --

	private void appendVar(final StringBuilder pre, //
		final String key, final Object value)
	{
		if (key.matches(".*[^a-zA-Z0-9_].*")) return; // illegal identifier
		if (value == null) return;
		final Object v;
		if (ij1Helper.isImagePlus(value)) {
			v = ij1Helper.getImageID(value);
		}
		else if (value instanceof File) {
			v = ((File) value).getAbsolutePath();
		}
		else v = value;
		if (v instanceof Number || v instanceof Boolean) {
			pre.append(key).append(" = ").append(v).append(";\n");
		}
		else {
			final String quoted = quote(v.toString());
			pre.append(key).append(" = \"").append(quoted).append("\";\n");
		}
	}

	private String quote(final String value) {
		String quoted = value.replaceAll("([\"\\\\])", "\\\\$1");
		quoted = quoted.replaceAll("\f", "\\\\f").replaceAll("\n", "\\\\n");
		quoted = quoted.replaceAll("\r", "\\\\r").replaceAll("\t", "\\\\t");
		return quoted;
	}

	private Object convertToImagePlus(final Object value) {
		if (value == null) return null;
		final int imageID;
		if (value instanceof Number) {
			imageID = ((Number) value).intValue();
		}
		else {
			try {
				imageID = Integer.parseInt(value.toString());
			}
			catch (final NumberFormatException exc) {
				return null;
			}
		}
		return ij1Helper.getImage(imageID);
	}

	// -- Helper classes --

	private static class IJ1MacroBindings extends HashMap<String, Object>
		implements Bindings
	{}
}
