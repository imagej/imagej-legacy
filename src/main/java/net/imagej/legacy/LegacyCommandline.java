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

import java.awt.GraphicsEnvironment;
import java.util.LinkedList;
import java.util.WeakHashMap;

import org.scijava.console.AbstractConsoleArgument;
import org.scijava.console.ConsoleArgument;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


/**
 * Handles the following ImageJ 1.x commands, as per {@link ij.ImageJ}'s
 * documentation:
 * <dl>
 * <dt>"file-name"</dt>
 * <dd>Opens a file<br>
 * Example 1: blobs.tif<br>
 * Example 2: /Users/wayne/images/blobs.tif<br>
 * Example 3: e81*.tif<br>
 * </dd>
 * <dt>-macro path [arg]</dt>
 * <dd>Runs a macro or script (JavaScript, BeanShell or Python), passing an
 * optional string argument, which the macro or script can be retrieve using the
 * getArgument() function. The macro or script is assumed to be in the
 * ImageJ/macros folder if 'path' is not a full directory path.<br>
 * Example 1: -macro analyze.ijm<br>
 * Example 2: -macro script.js /Users/wayne/images/stack1<br>
 * Example 2: -macro script.py '1.2 2.4 3.8'<br>
 * </dd>
 * <dt>-batch path [arg]</dt>
 * <dd>Runs a macro or script (JavaScript, BeanShell or Python) in batch (no
 * GUI) mode, passing it an optional argument. ImageJ exits when the macro
 * finishes.</dd>
 * <dt>-eval "macro code"</dt>
 * <dd>Evaluates macro code<br>
 * Example 1: -eval "print('Hello, world');"<br>
 * Example 2: -eval "return getVersion();"<br>
 * </dd>
 * <dt>-run command</dt>
 * <dd>Runs an ImageJ menu command<br>
 * Example: -run "About ImageJ..."</dd>
 * <dt>-ijpath path</dt>
 * <dd>Specifies the path to the directory containing the plugins directory<br>
 * Example: -ijpath /Applications/ImageJ<br>
 * </dd>
 * <dt>-port&lt;n&gt;</dt>
 * <dd>Specifies the port ImageJ uses to determine if another instance is
 * running<br>
 * Example 1: -port1 (use default port address + 1)<br>
 * Example 2: -port2 (use default port address + 2)<br>
 * Example 3: -port0 (don't check for another instance)<br>
 * </dd>
 * <dt>-debug</dt>
 * <dd>Runs ImageJ in debug mode</dd>
 * <dt>-batch-no-exit</dt>
 * <dd>Runs ImageJ in batch mode and disallows exiting the VM when done</dd>
 * </dl>
 * 
 * @author Johannes Schindelin
 */
public abstract class LegacyCommandline extends AbstractConsoleArgument {

	@Parameter
	protected LegacyService legacyService;

	@Parameter
	protected LogService log;

	private static class Flag extends WeakHashMap<LegacyService, Boolean> {
		public boolean isActive(final LegacyService service) {
			return Boolean.TRUE.equals(get(service));
		}
	}

	private static Flag batchMode = new Flag(), exitAtEnd = new Flag();

	protected IJ1Helper ij1Helper() {
		return legacyService.getIJ1Helper();
	}

	protected boolean isBatchMode() {
		final Boolean b = batchMode.get(legacyService);
		return b != null && b.booleanValue();
	}

	protected void handleBatchOption(final LinkedList<String> args) {
		if (args.isEmpty() || batchMode.get(legacyService) != null) return;
		if (args.contains("-batch-no-exit")) {
			exitAtEnd.put(legacyService, false);
		}
		else if (args.contains("-batch")) {
			exitAtEnd.put(legacyService, true);
		}
		else {
			batchMode.put(legacyService, false);
			return;
		}
		ij1Helper().invalidateInstance();
		ij1Helper().setBatchMode(true);
		batchMode.put(legacyService, true);
	}

	protected void handleBatchExit(final LinkedList<String> args) {
		if (!args.isEmpty() || !batchMode.isActive(legacyService)) {
			return;
		}

		legacyService.getContext().dispose();
		if (exitAtEnd.isActive(legacyService)) {
			System.exit(0);
		}
	}

	/** Handles {@code "file-name"}. */
	@Plugin(type = ConsoleArgument.class)
	public static class Filename extends LegacyCommandline {

		@Override
		public boolean supports(final LinkedList<String> args) {
			return !args.get(0).startsWith("-");
		}

		@Override
		public void handle(LinkedList<String> args) {
			if (!supports(args)) return;

			final String path = args.removeFirst(); // "file-name"

			handleBatchOption(args);
			ij1Helper().openImage(path, !GraphicsEnvironment.isHeadless() && !isBatchMode());
			handleBatchExit(args);
		}

	}

	/** Implements {@code -macro path [arg]}. */
	@Plugin(type = ConsoleArgument.class)
	public static class Macro extends LegacyCommandline {

		@Override
		public boolean supports(final LinkedList<String> args) {
			return args.size() > 1 && "-macro".equals(args.get(0));
		}

		@Override
		public void handle(LinkedList<String> args) {
			if (!supports(args)) return;

			args.removeFirst(); // -macro
			final String command = args.removeFirst();
			final String arg = args.isEmpty() ? "" : args.removeFirst();

			handleBatchOption(args);
			ij1Helper().runMacroFile(command, arg);
			handleBatchExit(args);
		}
		
	}

	/** Implements {@code -batch path [arg]} and {@code -batch-no-exit path [arg]}. */
	@Plugin(type = ConsoleArgument.class)
	public static class Batch extends LegacyCommandline {
		@Override
		public boolean supports(final LinkedList<String> args) {
			return "-batch".equals(args.get(0)) ||
				"-batch-no-exit".equals(args.get(0));
		}

		@Override
		public void handle(LinkedList<String> args) {
			if (!supports(args)) return;

			handleBatchOption(args);
			args.removeFirst(); // -batch or -batch-no-exit

			if (args.size() > 0) {
				final String path = args.removeFirst();
				final String arg = args.isEmpty() ? "" : args.removeFirst();
				ij1Helper().runMacroFile(path, arg);
			}
			handleBatchExit(args);
		}
	}

	/** Implements {@code -eval "macro code"}. */
	@Plugin(type = ConsoleArgument.class)
	public static class Eval extends LegacyCommandline {

		@Override
		public boolean supports(final LinkedList<String> args) {
			return args.size() > 1 && "-eval".equals(args.get(0));
		}

		@Override
		public void handle(LinkedList<String> args) {
			if (!supports(args)) return;

			args.removeFirst(); // -eval
			final String macro = args.removeFirst();

			handleBatchOption(args);
			ij1Helper().runMacro(macro);
			handleBatchExit(args);
		}
	}

	/** Implements {@code -run command}. */
	@Plugin(type = ConsoleArgument.class)
	public static class Run extends LegacyCommandline {

		@Override
		public boolean supports(final LinkedList<String> args) {
			return args.size() > 1 && "-run".equals(args.get(0));
		}

		@Override
		public void handle(LinkedList<String> args) {
			if (!supports(args)) return;

			args.removeFirst(); // -run
			final String command = args.removeFirst();

			handleBatchOption(args);
			ij1Helper().runMacro("run(\"" + quote(command) + "\", \"\");");
			handleBatchExit(args);
		}

		private String quote(final String value) {
			String quoted = value.replaceAll("([\"\\\\])", "\\\\$1");
			quoted = quoted.replaceAll("\f", "\\\\f").replaceAll("\n", "\\\\n");
			quoted = quoted.replaceAll("\r", "\\\\r").replaceAll("\t", "\\\\t");
			return quoted;
		}
	}

	/** Implements {@code -ijpath path}. */
	@Plugin(type = ConsoleArgument.class)
	public static class IJPath extends LegacyCommandline {

		@Override
		public boolean supports(final LinkedList<String> args) {
			return args.size() > 1 && "-ijpath".equals(args.get(0));
		}

		@Override
		public void handle(LinkedList<String> args) {
			if (!supports(args)) return;

			args.removeFirst(); // -ijpath
			final String ijPath = args.removeFirst();

			handleBatchOption(args);
			log.error("Skipping unsupported option -ijpath: " + ijPath);
			handleBatchExit(args);
		}
	}

	/** Implements {@code -port&lt;n&gt;}. */
	@Plugin(type = ConsoleArgument.class)
	public static class Port extends LegacyCommandline {

		@Override
		public boolean supports(final LinkedList<String> args) {
			return args.get(0).startsWith("-port");
		}

		@Override
		public void handle(LinkedList<String> args) {
			if (!supports(args)) return;

			final String option = args.removeFirst();

			handleBatchOption(args);
			log.error("Skipping unsupported option " + option);
			handleBatchExit(args);
		}
	}

	/** Implements {@code -debug}. */
	@Plugin(type = ConsoleArgument.class)
	public static class Debug extends LegacyCommandline {

		@Override
		public boolean supports(final LinkedList<String> args) {
			return "-debug".equals(args.get(0));
		}

		@Override
		public void handle(LinkedList<String> args) {
			if (!supports(args)) return;

			args.removeFirst(); // -debug

			handleBatchOption(args);
			ij1Helper().setDebugMode(true);
			handleBatchExit(args);
		}
	}

}
