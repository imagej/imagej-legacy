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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.awt.GraphicsEnvironment;

import ij.IJ;
import net.imagej.legacy.DefaultLegacyHooks;
import net.imagej.legacy.LegacyService;
import net.imagej.legacy.LegacyService;
import net.imagej.legacy.ui.LegacyUI;
import net.imagej.patcher.LegacyInjector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;

/**
 * Tests for verifying that ImageJ shuts down properly using
 * {@code System.exit(0)} when {@link ij.ImageJ#quit()} is called.
 * 
 * @author Curtis Rueden
 */
public class SystemExitIT {

	static {
		LegacyInjector.preinit();
	}

	public void testSystemExit() throws Exception {
		if (GraphicsEnvironment.isHeadless()) {
			System.err.println("Cannot run this test in headless mode; Skipping!");
			return;
		}

		final Context context = new Context(LegacyService.class, UIService.class);

		// verify that ImageJ1 is active
		assertNotNull(IJ.getInstance());

		// verify that the legacy layer is active
		assertNotNull(LegacyService.getInstance());

		// NB: Tell ImageJ1 to shut down the entire JVM when quitting.
		IJ.getInstance().exitWhenQuitting(true);

		// quit ImageJ1 the usual way, on a separate thread
		IJ.getInstance().quit();

		// wait for the JVM to terminate
		final long timeout = 10; // wait at most this many seconds for IJ1 to quit
		Thread.sleep(1000 * timeout);

		// you didn't save my life, you ruined my death
		fail("ImageJ1 failed to terminate the JVM after " + timeout + " seconds");
	}

	public static void main(final String... args) {
		try {
			final SystemExitIT systemExitIT = new SystemExitIT();
			systemExitIT.testSystemExit();
		}
		catch (final Throwable t) {
			t.printStackTrace(System.err);
			System.exit(1);
		}
	}

}
