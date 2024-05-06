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

import org.scijava.plugin.SciJavaPlugin;

/**
 * Extension point for actions to be executed after <i>Help&gt;Refresh
 * Menus</i>.
 * 
 * @author Johannes Schindelin
 */
public interface LegacyOpener extends SciJavaPlugin {

	/**
	 * Optionally override opening resources via legacy hooks.
	 * <p>
	 * This is intended as a "HandleExtraFileTypesPlus".
	 * </p>
	 * 
	 * @param path the path to the resource to open, or {@code null} if a dialog
	 *          needs to be shown
	 * @param planeIndex If applicable - the index of plane to open or -1 for all
	 *          planes
	 * @param displayResult if true, the opened object should be displayed before
	 *          returning
	 * @return The opened object, Boolean.TRUE if the open was canceled, or
	 *         {@code null} if the open failed.
	 */
	Object open(final String path, final int planeIndex,
		final boolean displayResult);
}
