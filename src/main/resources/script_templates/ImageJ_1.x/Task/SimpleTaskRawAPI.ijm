#@ Double (label = "Waiting time per step (s)", style = "0.000", value = "0.5") wait_s
#@ Integer(label = "Number of steps", value = "20") n_steps

taskName = "A task"

call("net.imagej.legacy.task.TaskHelper.removeAll", taskName); // Removes dangling tasks named identically generated during a macro crash for instance

call("net.imagej.legacy.task.TaskHelper.createTask", taskName);
call("net.imagej.legacy.task.TaskHelper.setProgressMaximum", taskName, n_steps);

call("net.imagej.legacy.task.TaskHelper.start", taskName);

for (i = 0; i<n_steps; i++) {
    call("net.imagej.legacy.task.TaskHelper.setProgressValue", taskName, i);
    wait(wait_s*1000.0);
}

call("net.imagej.legacy.task.TaskHelper.finish", taskName);
