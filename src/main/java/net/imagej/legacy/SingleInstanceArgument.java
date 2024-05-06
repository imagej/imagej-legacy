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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scijava.Priority;
import org.scijava.console.AbstractConsoleArgument;
import org.scijava.console.ConsoleArgument;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Handles arguments relating to the single instance listener feature of ImageJ
 * 1.x.
 * <p>
 * This {@link ConsoleArgument} plugin is unusual in that it tries to go first,
 * passing all command line arguments to an already-running instance of ImageJ
 * as appropriate. In cases where it does so successfully, it then shuts down
 * the application context.
 * </p>
 * <p>
 * The plugin scans the entire set of arguments to decide whether and how to
 * attempt to contact another running instance of ImageJ.
 * </p>
 * <p>
 * The {@code --forbid-single-instance} flag, in all cases, forbids ImageJ from
 * attempting to contact another running instance.
 * </p>
 * <p>
 * Conversely, the {@code --force-single-instance} flag encourages ImageJ to
 * forward arguments to another running instance even if it otherwise would not
 * do so.
 * </p>
 * <p>
 * The --portXXX argument sets the communication channel to use when
 * communicating with the other instance. The default is 7, but a different
 * value can be given to create multiple "single instances" of ImageJ on
 * different channels.
 * </p>
 * <p>
 * If neither {@code --forbid-single-instance} nor
 * {@code --force-single-instance} is given, the single instance logic is used
 * if all of the following conditions are met:
 * </p>
 * <ul>
 * <li><em>NOT</em> running in headless mode.</li>
 * <li>Neither the {@code -batch} nor {@code -ijpath} options have been
 * specified.</li>
 * <li>The "Run single instance listener" option in <em>Edit &gt; Options &gt;
 * Misc</em> menu is enabled (on macOS, this option is always disabled unless
 * toggled programmatically somehow in code).</li>
 * <li>Another instance of ImageJ is in fact running on the same channel (port 7
 * by default).</li>
 * </ul>
 * <p>
 * Finally, a note for the debugger: this plugin logs debug output for all code
 * paths, to make it easier to understand how and why the single instance logic
 * is invoked or skipped. One nice way to enable it in a targeted way is to pass
 * {@code -Dscijava.log.level:net.imagej.legacy=debug} on the "right hand side"
 * (i.e. after a {@code --} separator argument).
 * </p>
 * 
 * @author Curtis Rueden
 * @author Johannes Schindelin
 */
@Plugin(type = ConsoleArgument.class, priority = Priority.EXTREMELY_HIGH)
public class SingleInstanceArgument extends AbstractConsoleArgument {

	@Parameter(required = false)
	private LegacyService legacyService;

	@Parameter(required = false)
	private UIService uiService;

	/** Whether the handler has run yet. */
	private boolean initialized;

	/** The arguments not relevant to this argument handler. */
	private List<String> otherArgs;

	/** Whether the single instance logic should be forbidden from happening. */
	private boolean forbidSingleInstance;

	/** Whether the single instance logic should be invoked if at all possible. */
	private boolean forceSingleInstance;

	/** The port/channel of the already-running instance to contact. */
	private int port = 7;

	// -- ConsoleArgument methods --

	@Override
	public void handle(final LinkedList<String> args) {
		if (otherArgs == null) return;

		// Remove the handled arguments from the list, leaving only the others.
		args.clear();
		args.addAll(otherArgs);
		otherArgs = null;
	}

	// -- Typed methods --

	@Override
	public boolean supports(final LinkedList<String> args) {
		if (initialized) {
			// Already checked the args list in a prior run.
			return false;
		}
		initialized = true;

		// Scan the arguments, filtering out the relevant ones.
		// Retain any other arguments in an "other args" list.
		otherArgs = new ArrayList<>(args.size());
		for (final String arg : args) {
			if (!handle(arg)) otherArgs.add(arg);
		}

		// If list of remaining args differs from the full list,
		// return true, so the handle method can adjust it accordingly.
		final boolean result = args.size() != otherArgs.size();

		// If --forbid-single-instance override was passed, stop now!
		if (forbidSingleInstance) {
			log().debug("--forbid-single-instance given; skipping single instance logic.");
			return result;
		}

		// Check prerequisites. We need a fully functional
		// legacy service to defer to another instance.
		if (legacyService == null) {
			log().debug("No legacy service; skipping single instance logic.");
			return result;
		}
		final IJ1Helper ij1Helper = legacyService.getIJ1Helper();
		if (ij1Helper == null) {
			log().debug("No IJ1Helper; skipping single instance logic.");
			return result;
		}

		if (!forceSingleInstance) {
			// The --force-single-instance flag was not given,
			// so we'll let the heuristic decide whether to proceed.

			if (!ij1Helper.isRMIEnabled()) {
				// ImageJ 1.x's "Run single instance listener" option is not enabled!
				log().debug("RMI not enabled; skipping single instance logic.");
				return false;
			}

			// If in headless mode, stop now! This is vital when running
			// on cluster nodes, to avoid corruption of distributed jobs.
			if (Boolean.getBoolean("java.awt.headless") || //
				uiService != null && uiService.isHeadless())
			{
				log().debug("Headless mode; skipping single instance logic.");
				return result;
			}

			// Some arguments preclude deferring to another instance.
			final String[] blocklistedArgs = {"-batch", "-ijpath", "--headless"};
			for (final String arg : otherArgs) {
				for (final String blocklisted : blocklistedArgs) {
					if (blocklisted.equals(arg)) {
						log().debug("Skipping single instance logic due to argument: " + arg);
						return result;
					}
				}
			}
		}

		// Everything looks good so far; let's check for another instance.
		log().debug("Invoking single instance logic.");
		final SingleInstance instance = new SingleInstance(port, log(), ij1Helper);
		if (instance.sendArguments(listToArray(otherArgs))) {
			log().info("Detected existing ImageJ; passing arguments along");

			// All arguments were passed to the other instance; this instance
			// should not do any additional argument handling whatsoever.
			otherArgs = Collections.emptyList();

			// And this instance should now terminate.
			context().dispose();
		}

		return result;
	}

	// -- Helper methods --

	private boolean handle(final String arg) {
		if ("--forbid-single-instance".equals(arg)) {
			forbidSingleInstance = true;
			log().debug("Single instance mode DISABLED");
			return true;
		}
		if ("--force-single-instance".equals(arg)) {
			forceSingleInstance = true;
			log().debug("Single instance mode ENABLED");
			return true;
		}
		// --port
		final Pattern p = Pattern.compile("--?port([0-9]+)");
		final Matcher m = p.matcher(arg);
		if (!m.matches()) return false;
		final String portString = m.group(1);
		try {
			port = Integer.parseInt(portString);
			log().debug("Single instance port -> " + port);
		}
		catch (final NumberFormatException exc) {
			log().debug("Invalid single instance port: " + portString);
		}
		return true;
	}

	private String[] listToArray(List<String> list) {
		final String[] array = new String[list.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = list.get(i);
		return array;
	}
}
