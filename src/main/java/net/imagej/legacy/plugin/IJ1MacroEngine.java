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

package net.imagej.legacy.plugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.script.ScriptException;

import net.imagej.legacy.IJ1Helper;

import org.scijava.script.AbstractScriptEngine;

/**
 * An almost JSR-223-compliant script engine for the ImageJ 1.x macro language.
 * <p>
 * As far as possible, this script engine conforms to JSR-223. It lets the user
 * evaluate ImageJ 1.x macros. Due to the ImageJ 1.x macro interpreter's
 * limitations, functionality such as the {@link #put(String, Object)} is not
 * supported, however.
 * </p>
 * 
 * @author Johannes Schindelin
 */
public class IJ1MacroEngine extends AbstractScriptEngine {

	private final IJ1Helper ij1Helper;

	/**
	 * Constructs an ImageJ 1.x macro engine.
	 * 
	 * @param ij1Helper
	 *            the helper to evaluate the macros
	 */
	public IJ1MacroEngine(final IJ1Helper ij1Helper) {
		this.ij1Helper = ij1Helper;
	}

	@Override
	public Object eval(final String macro) throws ScriptException {
		return ij1Helper.runMacro(macro);
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
		} catch (final IOException e) {
			throw new ScriptException(e);
		}
		return eval(builder.toString());
	}

	@Override
	public void put(final String key, final Object value) {
		if (FILENAME.equals(key)) {
			return; // ignore
		}
		final String message = "Unhandled key: " + key + ": " + value;
		final Writer writer = getContext().getErrorWriter();
		if (writer != null) {
			try {
				writer.write(message + "\n");
				return;
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		System.err.println(message);
	}
}
