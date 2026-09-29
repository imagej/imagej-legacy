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

import java.util.IdentityHashMap;
import java.util.Map;

import net.imagej.legacy.IJ1Helper;
import net.imagej.legacy.LegacyService;

import org.scijava.event.EventHandler;
import org.scijava.task.Task;
import org.scijava.task.event.TaskEvent;

/**
 * Aggregates the progress of all running {@link Task}s into the single
 * progress bar and status message of the original ImageJ user interface.
 * <p>
 * All tasks are weighted by their own progress maximum. Finished tasks keep
 * contributing until <em>no</em> task is running anymore, and the displayed
 * value never decreases in the meantime. This prevents the bar from jumping
 * around when tasks start and finish while others are still running.
 * </p>
 *
 * @author Curtis Rueden
 */
public class TaskStatusAggregator {

	private static final int RESOLUTION = 100;

	private final LegacyService legacyService;
	private final IJ1Helper helper;

	/** Progress state of each task in the current batch. */
	private final Map<Task, double[]> batch = new IdentityHashMap<>();

	private int lastPercent = -1;
	private String lastStatus;

	public TaskStatusAggregator(final LegacyService legacyService,
		final IJ1Helper helper)
	{
		this.legacyService = legacyService;
		this.helper = helper;
		legacyService.context().inject(this);
	}

	@EventHandler
	private synchronized void onEvent(final TaskEvent evt) {
		final Task task = evt.getTask();
		if (task.isDone()) {
			final double[] p = batch.get(task);
			if (p != null) {
				p[1] = Math.max(p[1], 1);
				p[0] = p[1];
				p[2] = 1; // finished
			}
		}
		else {
			final double max = task.getProgressMaximum();
			// Note: tasks without a known maximum are indeterminate; we count them
			// as a unit of work that completes only when they finish.
			final double[] p = batch.computeIfAbsent(task, t -> new double[3]);
			p[1] = max > 0 ? max : 1;
			p[0] = max > 0 ? Math.min(task.getProgressValue(), max) : 0;
		}
		update();
	}

	private void update() {
		int running = 0;
		double done = 0, total = 0;
		Task only = null;
		for (final Map.Entry<Task, double[]> e : batch.entrySet()) {
			final double[] p = e.getValue();
			// Normalize so each task has equal weight, regardless of its units.
			done += p[0] / p[1];
			total += 1;
			if (p[2] == 0) {
				running++;
				only = e.getKey();
			}
		}

		final boolean finished = running == 0;
		final String status = finished ? null : running == 1 ? statusOf(only) //
			: running + " tasks running";
		int percent = total == 0 ? 0 : (int) Math.round(RESOLUTION * done / total);
		if (percent < lastPercent) percent = lastPercent; // never go backwards

		final boolean wasProcessing = legacyService.setProcessingEvents(true);
		try {
			if (finished) {
				batch.clear();
				if (lastPercent >= 0) helper.setProgress(RESOLUTION, RESOLUTION);
				if (lastStatus != null) helper.setStatus("");
				lastPercent = -1;
				lastStatus = null;
				return;
			}
			if (percent != lastPercent) {
				lastPercent = percent;
				helper.setProgress(percent, RESOLUTION);
			}
			if (status != null && !status.equals(lastStatus)) {
				lastStatus = status;
				helper.setStatus(status);
			}
		}
		finally {
			legacyService.setProcessingEvents(wasProcessing);
		}
	}

	private static String statusOf(final Task task) {
		final String msg = task.getStatusMessage();
		final String name = task.getName();
		if (msg == null || msg.isEmpty()) return name;
		return name == null || name.isEmpty() ? msg : name + ": " + msg;
	}
}
