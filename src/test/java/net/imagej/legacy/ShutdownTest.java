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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import ij.IJ;

import java.awt.GraphicsEnvironment;

import net.imagej.legacy.ui.LegacyUI;
import net.imagej.patcher.LegacyInjector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.ui.UIService;
import org.scijava.ui.UserInterface;

/**
 * Tests for verifying that ImageJ shuts down properly under various
 * circumstances.
 * 
 * @author Curtis Rueden
 */
public class ShutdownTest {

	static {
		LegacyInjector.preinit();
	}

	private Context context;
	private ClassLoader originalLoader;

	@Before
	public void setUp() {
		assumeTrue(!GraphicsEnvironment.isHeadless());

		// NB: Save a reference to the context class loader _before_ the test.
		// This will help avoid class loaders bleeding from one test to another.
		originalLoader = Context.getClassLoader();

		context = new Context(LegacyService.class, UIService.class);
	}

	@After
	public void tearDown() {
		if (GraphicsEnvironment.isHeadless()) return;

		context.dispose();
		context = null;

		// NB: Restore the _original_ context class loader, from before the test.
		// This avoids class loaders bleeding from one test to another.
		Thread.currentThread().setContextClassLoader(originalLoader);
	}

	/** Tests {@link ij.ImageJ#quit()}. */
	@Test
	public void testImageJ1Quit() throws Exception {
		final Thread[] quitThread = new Thread[1];

		// tweak the legacy hooks to record IJ1's Quit thread
		LegacyInjector.installHooks(Thread.currentThread().getContextClassLoader(),
			new DefaultLegacyHooks(context.service(LegacyService.class)) {
			@Override
			public boolean disposing() {
				synchronized (quitThread) {
					quitThread[0] = Thread.currentThread();
					quitThread.notify();
				}
				return super.disposing();
			}
		});

		// verify that ImageJ1 is active
		assertNotNull(IJ.getInstance());

		// verify that the legacy layer is active
		assertNotNull(LegacyService.getInstance());

		// verify that there is no IJ1 Quit thread
		assertNull(quitThread[0]);

		// NB: Tell ImageJ1 not to shut down the entire JVM when quitting. We need
		// to do this because surefire and failsafe do not support System.exit
		// being called, even from a forked JVM. So we cannot test ImageJ1's normal
		// shutdown behavior. But we can test with exitWhenQuitting set to false.
		IJ.getInstance().exitWhenQuitting(false);

		// quit ImageJ1 the usual way, on a separate thread
		IJ.getInstance().quit();

		final long timeout = 10; // maximum timeout during ImageJ1 shutdown

		// verify that there is an IJ1 Quit thread now
		synchronized (quitThread) {
			if (quitThread[0] == null) quitThread.wait(1000 * timeout);
		}
		assertNotNull("ImageJ1 Quit thread is not set", quitThread[0]);

		// verify that IJ1 indeed spawned a new thread to quit
		if (quitThread[0] == Thread.currentThread()) {
			fail("ImageJ1 is not quitting on a new thread");
		}

		// wait for the IJ1 Quit thread to terminate
		quitThread[0].join(1000 * timeout);
		if (quitThread[0].isAlive()) {
			fail("ImageJ1 failed to quit after " + timeout + " seconds");
		}

		// verify that ImageJ1 has shut down
		assertNull(IJ.getInstance());

		// verify that the legacy layer has been disposed
		assertNull(LegacyService.getInstance());
	}

	/** Tests {@link LegacyService#dispose()}. */
	@Test
	public void testLegacyServiceDispose() {
		// verify that ImageJ1 is active
		assertNotNull(IJ.getInstance());

		// verify that the legacy layer is active
		assertNotNull(LegacyService.getInstance());

		// dispose the legacy service
		context.service(LegacyService.class).dispose();

		// verify that ImageJ1 has shut down
		assertNull(IJ.getInstance());

		// verify that the legacy layer is inactive
		assertNull(LegacyService.getInstance());
	}

	/** Tests {@link LegacyUI#dispose()}. */
	@Test
	public void testLegacyUIDispose() {
		// verify that ImageJ1 is active
		assertNotNull(IJ.getInstance());

		// verify that the legacy layer is active
		assertNotNull(LegacyService.getInstance());

		// dispose the legacy user interface
		final UserInterface legacyUI =
			context.service(UIService.class).getUI(LegacyUI.NAME);
		legacyUI.dispose();

		// verify that ImageJ1 has shut down
		assertNull(IJ.getInstance());

		// verify that the legacy layer is still active
		assertNotNull(LegacyService.getInstance());
	}

}
