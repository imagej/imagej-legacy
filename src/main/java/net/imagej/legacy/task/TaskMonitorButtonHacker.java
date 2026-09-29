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

package net.imagej.legacy.task;

import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.prefs.PrefService;
import org.scijava.ui.swing.task.SwingTaskMonitorComponent;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.plaf.LayerUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
		buttonTaskMonitor.setToolTipText("Tasks: click to show running tasks");

		// Overlay a task list icon, so the button is recognizable when idle.
		final JLayer<JComponent> layer = new JLayer<>(buttonTaskMonitor,
			new TaskListIconUI());
		layer.setToolTipText(buttonTaskMonitor.getToolTipText());
		panel.add(layer, "height 20:20:20");

		return layer;
	}

	/** Paints a small "bulleted list" glyph centered on top of the view. */
	private static class TaskListIconUI extends LayerUI<JComponent> {

		@Override
		public void paint(final Graphics g, final JComponent c) {
			super.paint(g, c);
			final Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(c.getForeground() == null ? Color.DARK_GRAY
					: c.getForeground());
				g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));
				final double s = Math.min(c.getWidth(), c.getHeight());
				final double cx = c.getWidth() / 2.0, cy = c.getHeight() / 2.0;
				final double dot = s * 0.07, gap = s * 0.16;
				for (int i = -1; i <= 1; i++) {
					final double y = cy + i * gap;
					g2.fill(new java.awt.geom.Ellipse2D.Double(cx - s * 0.22 - dot,
						y - dot, 2 * dot, 2 * dot));
					g2.draw(new java.awt.geom.Line2D.Double(cx - s * 0.10, y,
						cx + s * 0.22, y));
				}
			}
			finally {
				g2.dispose();
			}
		}
	}

}
