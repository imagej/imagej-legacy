/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
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

import java.util.LinkedList;
import java.util.List;

import org.scijava.Priority;
import org.scijava.console.ConsoleArgument;
import org.scijava.console.ConsoleService;
import org.scijava.console.DefaultConsoleService;
import org.scijava.console.OutputEvent;
import org.scijava.console.OutputListener;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.service.ServiceIndex;

@Plugin(type = Service.class, priority = Priority.HIGH_PRIORITY)
public class LegacyConsoleService extends AbstractService implements ConsoleService {

	@Parameter
	private LegacyService legacy;

	@Parameter
	private LogService log;

	private ConsoleService consoleService;
	private int port = 7;

	@Override
	public void processArgs(final String... args) {
		if (legacy != null && singleInstancePermissible(args)) {
			final IJ1Helper helper = legacy.getIJ1Helper();
			if (helper != null) {
				final SingleInstance instance = new SingleInstance(port, log, helper);
				if (helper.isRMIEnabled() && instance.sendArguments(args)) {
					log.info("Detected existing ImageJ; passing arguments along");
					context().dispose();
					return;
				}
			}
		}

		consoleService().processArgs(args);
	}

	// -- ConsoleService API --

	@Override
	public ConsoleArgument getHandler(LinkedList<String> data) {
		return consoleService().getHandler(data);
	}

	@Override
	public List<ConsoleArgument> getInstances() {
		return consoleService().getInstances();
	}

	@Override
	public boolean supports(LinkedList<String> data) {
		return consoleService().supports(data);
	}

	@Override
	public <P extends ConsoleArgument> P getInstance(Class<P> pluginClass) {
		return consoleService().getInstance(pluginClass);
	}

	@Override
	public PluginService getPluginService() {
		return consoleService().getPluginService();
	}

	@Override
	public List<PluginInfo<ConsoleArgument>> getPlugins() {
		return consoleService().getPlugins();
	}

	@Override
	public Class<ConsoleArgument> getPluginType() {
		return consoleService().getPluginType();
	}

	@Override
	public <P extends ConsoleArgument> P create(Class<P> pluginClass) {
		return consoleService().create(pluginClass);
	}

	@Override
	public Class<LinkedList<String>> getType() {
		return consoleService().getType();
	}

	@Override
	public void addOutputListener(OutputListener l) {
		consoleService().addOutputListener(l);
	}

	@Override
	public void removeOutputListener(OutputListener l) {
		consoleService().removeOutputListener(l);
	}

	@Override
	public void notifyListeners(OutputEvent event) {
		consoleService().notifyListeners(event);
	}

	// -- Helpers --

	/**
	 * Detects arguments that prevent handing off to existing instances.
	 * 
	 * @param args arguments, as passed to the console service
	 * @return true if we should look for an existing instance to use
	 */
	private boolean singleInstancePermissible(final String... args) {
		for (final String arg : args) {
			// should not happen, but we're not called by main() but by processArgs()
			if (arg == null) continue;
			if (arg.equals("-batch") || arg.equals("-ijpath")) return false;
			if (arg.startsWith("-port")) try {
				port = Integer.parseInt(arg.substring(5), 10);
				if (port == 0) return false;
			} catch (NumberFormatException e) {
				// ImageJ1 parses these as 0, disabling the single instance functionality
				return false;
			}
		}

		return true;
	}

	/**
	 * Lazy helper method to allow {@link DefaultConsoleService} to perform much
	 * of the work of this class, despite being lower priority.
	 *
	 * @return Active {@code ConsoleService}.
	 */
	private ConsoleService consoleService() {
		if (consoleService == null) {
			final ServiceIndex index = getContext().getServiceIndex();

			// Set datasetService to the next highest priority service
			consoleService =
				index.getNextService(ConsoleService.class, LegacyConsoleService.class);
		}
		return consoleService;
	}
}
