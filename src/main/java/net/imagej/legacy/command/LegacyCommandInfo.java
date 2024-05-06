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

package net.imagej.legacy.command;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.scijava.MenuPath;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.util.Manifest;
import org.scijava.util.Types;

/**
 * Metadata about an ImageJ 1.x {@code PlugIn}.
 *
 * @author Curtis Rueden
 */
public class LegacyCommandInfo extends CommandInfo {

	private static final String LEGACY_PLUGIN_ICON = "/icons/legacy.png";

	private final String className;
	private final String arg;
	private final ClassLoader classLoader;
	private final String id;

	private Class<?> ij1Class;

	public LegacyCommandInfo(final MenuPath menuPath,
		final String className, final String arg, final ClassLoader classLoader)
	{
		super(LegacyCommand.class);
		this.className = className;
		this.arg = arg;
		this.classLoader = classLoader;

		// HACK: Make LegacyCommands a subtype of regular Commands.
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final Class<Command> legacyCommandClass = (Class) LegacyCommand.class;
		setPluginType(legacyCommandClass);

		final Map<String, Object> presets = new HashMap<>();
		presets.put("className", className);
		presets.put("arg", arg);
		setPresets(presets);

		if (menuPath != null) {
			setMenuPath(menuPath);

			// flag legacy command with special icon
			setIconPath(LEGACY_PLUGIN_ICON);
		}

		id = "legacy:" + className + (appendArg() ? "(\"" + arg + "\")" : "");
	}

	// -- LegacyCommandInfo methods --

	/** Gets the name of the class backing this legacy command. */
	public String getLegacyClassName() {
		return className;
	}

	/** Gets the {@code arg} portion of the legacy command, if any. */
	public String getArg() {
		return arg;
	}

	/** Gets the class backing this legacy command, loading it as needed. */
	public Class<?> loadLegacyClass() {
		if (ij1Class == null) initCommandClass();
		return ij1Class;
	}

	// -- Identifiable methods --

	@Override
	public String getIdentifier() {
		return id;
	}

	// -- Locatable methods --

	@Override
	public String getLocation() {
		final Class<?> c = loadLegacyClass();
		if (c == null) return "<unknown>";
		final URL url = Types.location(c);
		return url == null ? null : url.toExternalForm();
	}

	// -- Versioned methods --

	@Override
	public String getVersion() {
		final Class<?> c = loadLegacyClass();
		if (c == null) return "<unknown>";
		final Manifest m = Manifest.getManifest(c);
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
	private boolean appendArg() {
		if (arg == null || arg.isEmpty()) return false; // no need
		if (className.equals("ij.plugin.Animator")) return true;
		if (className.equals("ij.plugin.Commands")) return true;
		if (className.equals("ij.plugin.Converter")) return true;
		if (className.equals("ij.plugin.StackEditor")) return true;
		if (className.startsWith("ij.plugin.filter.")) return true;
		return false;
	}

	private synchronized void initCommandClass() {
		if (ij1Class != null) return;
		ij1Class = Types.load(className, classLoader, true);
	}
}
