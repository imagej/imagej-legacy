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

package net.imagej.legacy;

import static org.junit.Assume.assumeTrue;

import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.net.URLClassLoader;

import net.imagej.patcher.LegacyClassLoader;
import net.imagej.patcher.LegacyEnvironment;
import net.imagej.patcher.LegacyInjector;

import org.junit.Test;
import org.scijava.annotations.EclipseHelper;

/**
 * Ensures that <i>Help>Switch to Modern Mode</i> in patched ImageJ 1.x works as
 * advertised.
 * 
 * @author Johannes Schindelin
 */
public class SwitchToModernTest {

	static {
		LegacyInjector.preinit();
	}

	@Test
	public void testSwitchToModernMode() throws Exception {
		assumeTrue(!GraphicsEnvironment.isHeadless());

		final Thread thread = Thread.currentThread();
		final ClassLoader savedContextClassLoader = thread.getContextClassLoader();
		EclipseHelper.updateAnnotationIndex(savedContextClassLoader);

		try {
			final ClassLoader thisLoader = getClass().getClassLoader();
			final URL[] urls = thisLoader instanceof URLClassLoader ? ((URLClassLoader)thisLoader).getURLs() : new URL[0];
			final ClassLoader loader = new LegacyClassLoader(false) {
				{
					for (final URL url : urls) {
						addURL(url);
					}
				}
			};
			final LegacyEnvironment ij1 = new LegacyEnvironment(loader, true);
			ij1.disableIJ1PluginDirs();
			ij1.disableInitializer();
			ij1.noPluginClassLoader();
			ij1.suppressIJ1ScriptDiscovery();
			ij1.runMacro("call(\"ij.IJ.redirectErrorMessages\");", "");
			ij1.run("Switch to Modern Mode", "");
		} finally {
			thread.setContextClassLoader(savedContextClassLoader);
		}
	}
}
