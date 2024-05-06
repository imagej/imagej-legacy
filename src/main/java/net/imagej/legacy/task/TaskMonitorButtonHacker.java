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

package net.imagej.legacy.task;

import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.prefs.PrefService;
import org.scijava.ui.swing.task.SwingTaskMonitorComponent;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.Arrays;

/**
 * Hacks the ImageJ task monitor bar into place, see {@link SwingTaskMonitorComponent}
 * @author Curtis Rueden, Nicolas Chiaruttini
 */
public class TaskMonitorButtonHacker {

	private final Context context;

	public TaskMonitorButtonHacker(final Context context) {
		this.context = context;
	}

	/** Adds a task progress monitor bar to the main image frame. */
	public Object addTaskBar(final Object imagej) {
		if (!(imagej instanceof Window)) return null; // NB: Avoid headless issues.
		// Retrieve user preferences.
		// HACK: We cannot use OptionsService here, because it will create
		// all the OptionsPlugin instances before needed services are ready.
		// While this is indicative of a design issue with OptionsService and
		// maybe SciJava Common's application framework in general, the best we
		// can do here is to extract the persisted options in a lower-level way.
		final PrefService prefService = context.getService(PrefService.class);
		boolean estimateTime = true;
		boolean confirmCancel = true;
		boolean mini = true;
		if (prefService != null) {
			final String style = prefService.get(TaskMonitorOptions.class, "style");
			if ("Disable".equals(style)) return null; // task monitor disabled
			estimateTime = !("Disable".equals(prefService.get(TaskMonitorOptions.class, "estimateTime")));
			confirmCancel = !("Disable".equals(prefService.get(TaskMonitorOptions.class, "confirmCancel")));
			mini = "Mini".equals(prefService.get(TaskMonitorOptions.class, "style"));
		}

		final Component[] ijc = ((Container) imagej).getComponents();
		if (ijc.length < 2) return null;
		final Component ijc1 = ijc[1];
		if (!(ijc1 instanceof Container)) return null;

		// rebuild the main panel (status label + progress bar)
		final Container panel = (Container) ijc1;
		final Component[] pc = panel.getComponents();
		panel.removeAll();
		panel.setLayout(new MigLayout("fillx, insets 0", "[0:0]p![p!]"));

		Arrays.stream(pc).forEach(panel::add);

		JComponent buttonTaskMonitor = new SwingTaskMonitorComponent(context,estimateTime,confirmCancel, 20, mini).getComponent();
		buttonTaskMonitor.setDoubleBuffered(true); // Flickers otherwise
		panel.add(buttonTaskMonitor,"height 20:20:20");

		return buttonTaskMonitor;
	}

}
