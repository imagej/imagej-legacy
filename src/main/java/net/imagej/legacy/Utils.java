/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
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

import net.imagej.legacy.plugin.LegacyThreadGroup;

/**
 * Static utility methods used in the {@link net.imagej.legacy} package.
 * 
 * @author Barry DeZonia
 */
public class Utils {

	/**
	 * Returns true if the given thread is in any way a child of a
	 * {@link LegacyThreadGroup}. Returns false otherwise.
	 */
	public static boolean isLegacyThread(Thread t) {
		return findLegacyThreadGroup(t) != null;
	}

	/**
	 * If the given thread is not derived from a LegacyCommand returns null. Else
	 * it returns the ThreadGroup at the base of the LegacyCommand.
	 */
	public static LegacyThreadGroup findLegacyThreadGroup(Thread t) {
		for (ThreadGroup group = t.getThreadGroup(); group != null; group = group.getParent()) {
			if (group instanceof LegacyThreadGroup) return (LegacyThreadGroup)group;
		}
		return null;
	}

	@Deprecated
	public static boolean isLegacyMode(final LegacyService legacyService) {
		return legacyService == null || legacyService.isLegacyMode();
	}

}
