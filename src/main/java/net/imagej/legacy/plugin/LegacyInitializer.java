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

package net.imagej.legacy.plugin;

import ij.IJ;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import net.imagej.legacy.IJ1Helper;

import org.scijava.Context;
import org.scijava.ui.UIService;

/**
 * The <i>ij1-patcher</i> defaults to running this class whenever a new
 * {@code PluginClassLoader} is initialized.
 * 
 * @author Johannes Schindelin
 */
public class LegacyInitializer implements Runnable {

	private final static ThreadLocal<Class<?>> semaphore =
			new ThreadLocal<>();

	@Override
	public void run() {
		if (getClass() == semaphore.get()) return;
		semaphore.set(getClass());
		final ClassLoader loader = IJ.getClassLoader();
		Thread.currentThread().setContextClassLoader(loader);
		if (!GraphicsEnvironment.isHeadless() &&
			!SwingUtilities.isEventDispatchThread()) try {
			SwingUtilities.invokeAndWait(new Runnable() {

				@Override
				public void run() {
					Thread.currentThread().setContextClassLoader(loader);
				}
			});
		}
		catch (final InvocationTargetException e) {
			e.printStackTrace();
		}
		catch (final InterruptedException e) {
			// ignore
		}

		try {
			final Object ij1 = IJ.getInstance();
			final Context context = IJ1Helper.getLegacyContext();
			if (ij1 != null && context != null) {
				final UIService ui = context.getService(UIService.class);
				if (ui != null) ui.showUI();
			}
		}
		catch (final Throwable t) {
			// do nothing; we're not in the PluginClassLoader's class path
			return;
		}
		finally {
			semaphore.remove();
		}
	}

}
