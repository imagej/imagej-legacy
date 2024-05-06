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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.scijava.module.Module;

/**
 * Globally accessible data structure that stores, for each {@link Module}
 * instance, the set of "pre-resolved" module inputs.
 * <p>
 * The {@link MacroRecorderPreprocessor} populates this structure with the set
 * of pre-resolved inputs for its module. The {@link MacroRecorderPostprocessor}
 * then accesses this same set so that it can record only those input values
 * that were still unresolved at that point in the preprocessing chain.
 * </p>
 * 
 * @author Curtis Rueden
 */
public final class MacroRecorderExcludedInputs {

	private MacroRecorderExcludedInputs() {
		// Prevent instantiation of utility class.
	}

	private static final Map<Module, Set<String>> EI_MAP =
		Collections.synchronizedMap(new WeakHashMap<>());

	public static Set<String> create(final Module module) {
		return EI_MAP.computeIfAbsent(module, key -> new HashSet<>());
	}

	/**
	 * Obtains the cached set of excluded inputs for a particular module instance,
	 * clearing it from the data structure.
	 */
	public static Set<String> retrieve(final Module module) {
		return EI_MAP.remove(module);
	}
}
