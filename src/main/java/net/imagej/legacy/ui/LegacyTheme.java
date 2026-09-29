/*
 * #%L
 * ImageJ2 software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2026 ImageJ2 developers.
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
package net.imagej.legacy.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;

import ij.gui.ProgressBar;

import javax.swing.UIManager;

/**
 * Utility methods for making the (AWT-based) original ImageJ window respect
 * the current Swing look and feel, which the original ImageJ does not do.
 *
 * @author Curtis Rueden
 */
public final class LegacyTheme {

	private LegacyTheme() {}

	/** Whether the current Swing look and feel has a dark background. */
	public static boolean isDark() {
		final Color bg = UIManager.getColor("Panel.background");
		return bg != null && isDark(bg);
	}

	public static boolean isDark(final Color c) {
		// see https://stackoverflow.com/a/3943023
		return (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) < 186;
	}

	/**
	 * Adapts the given legacy status bar to the look and feel. Note: The status
	 * line is a JLabel, so its text color already follows the look and feel, but
	 * the AWT panel behind it has a hardcoded light background. We only touch it
	 * for dark themes, to leave the standard appearance untouched.
	 */
	public static void applyToStatusBar(final Component statusBar) {
		if (statusBar == null || !isDark()) return;
		statusBar.setBackground(UIManager.getColor("Panel.background"));
		final Color fg = UIManager.getColor("Label.foreground");
		if (fg != null) statusBar.setForeground(fg);
		if (statusBar instanceof Container) {
			for (final Component c : ((Container) statusBar).getComponents()) {
				if (c instanceof ProgressBar) applyToProgressBar((ProgressBar) c);
			}
		}
	}

	/**
	 * HACK: The legacy progress bar paints its idle state as a rectangle in a
	 * private, hardcoded light color, which is visible in dark themes.
	 */
	private static void applyToProgressBar(final ProgressBar bar) {
		final Color bg = UIManager.getColor("Panel.background");
		setField(bar, "backgroundColor", bg);
		setField(bar, "frameBrighter", bg.brighter());
		setField(bar, "frameDarker", bg.darker());
		bar.setBackground(bg);
		bar.repaint();
	}

	private static void setField(final Object o, final String name,
		final Color value)
	{
		try {
			final Field f = o.getClass().getDeclaredField(name);
			f.setAccessible(true);
			f.set(o, value);
		}
		catch (final ReflectiveOperationException | RuntimeException exc) {
			// NB: Cosmetic only; leave the default colors.
		}
	}
}
