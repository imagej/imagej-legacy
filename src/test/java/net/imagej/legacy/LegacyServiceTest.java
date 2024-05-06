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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import ij.IJ;

import net.imagej.patcher.LegacyInjector;

import org.junit.After;
import org.junit.Test;
import org.scijava.Context;

/**
 * Unit tests for {@link LegacyService}.
 * 
 * @author Johannes Schindelin
 */
public class LegacyServiceTest {

	static {
		/*
		 * We absolutely require that the LegacyInjector did its job before we
		 * use the ImageJ 1.x classes here, in case the LegacyService tests did
		 * not run yet, so that the classes are properly patched before use.
		 * 
		 * Just loading the class is not enough; it will not get initialized. So
		 * we call the preinit() method just to force class initialization (and
		 * thereby the LegacyInjector to patch ImageJ 1.x).
		 */
		LegacyInjector.preinit();
	}

	private Context context;

	@After
	public void disposeContext() {
		if (context != null) {
			context.dispose();
			context = null;
		}
	}

	@Test
	public void testContext() {
		context = new Context(LegacyService.class);
		final LegacyService legacyService =
			context.getService(LegacyService.class);
		assertTrue(legacyService != null);

		Context context2 = (Context)IJ.runPlugIn(Context.class.getName(), null);
		assertNotNull(context2);
		assertSame(context, context2);
	}

	@Test
	public void testContextWasDisposed() {
		context = new Context(LegacyService.class);
		final LegacyService legacyService =
			context.getService(LegacyService.class);
		assertTrue(legacyService != null);
	}

}

