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

import java.net.URL;

import org.scijava.AbstractUIDetails;
import org.scijava.Identifiable;
import org.scijava.Locatable;
import org.scijava.Versioned;
import org.scijava.util.ClassUtils;
import org.scijava.util.Manifest;

/**
 * Metadata about an ImageJ 1.x {@code PlugIn}.
 *
 * @author Curtis Rueden
 */
public class LegacyPlugInInfo extends AbstractUIDetails implements
	Identifiable, Locatable, Versioned
{

	private final Class<?> clazz;
	private final String id;

	public LegacyPlugInInfo(final String className, final String arg,
		final ClassLoader classLoader)
	{
		try {
			clazz = classLoader.loadClass(className);
		}
		catch (final ClassNotFoundException exc) {
			throw new IllegalArgumentException(exc);
		}
		id = "legacy:" + className + (appendArg(className, arg) ? "?" + arg : "");
	}

	// -- Identifiable methods --

	@Override
	public String getIdentifier() {
		return id;
	}

	// -- Locatable methods --

	@Override
	public String getLocation() {
		final URL url = ClassUtils.getLocation(clazz);
		return url == null ? null : url.toExternalForm();
	}

	// -- Versioned methods --

	@Override
	public String getVersion() {
		final Manifest m = Manifest.getManifest(clazz);
		return m == null ? null : m.getImplementationVersion();
	}

	// -- Helper methods --

	/**
	 * Whether to append the {@code arg} parameter to the identifier.
	 * <p>
	 * Some legacy commands use the {@code arg} parameter as a subcommand to
	 * execute, and are known not to contain any user-specific details such as
	 * file paths. The most classic example is {code ij.plugin.Commands}. We want
	 * to record such subcommands when feasible, but not at the expense of user
	 * privacy.
	 * </p>
	 */
	private boolean appendArg(final String className, final String arg) {
		if (arg == null || arg.isEmpty()) return false; // no need
		if (className.equals("ij.plugin.Animator")) return true;
		if (className.equals("ij.plugin.Commands")) return true;
		if (className.equals("ij.plugin.Converter")) return true;
		if (className.equals("ij.plugin.StackEditor")) return true;
		if (className.startsWith("ij.plugin.filter.")) return true;
		return false;
	}

}
