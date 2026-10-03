#@ Double (label = "Waiting time per step (s)", style = "0.000", value = "0.5") wait_s
#@ Integer(label = "Number of steps", value = "20") n_steps

taskName = "A task";

taskCreate(taskName, n_steps);

for (i = 0; i<n_steps; i++) {
    taskSetProgressValue(taskName, i);
    wait(wait_s*1000.0);
}

taskFinish(taskName);

// --------------------------- FUNCTIONS

function taskCreate(name, maxSteps) {
    call("net.imagej.legacy.task.TaskHelper.removeAll", name); // Removes dangling tasks named identically generated during a macro crash for instance
    call("net.imagej.legacy.task.TaskHelper.createTask", name);
    call("net.imagej.legacy.task.TaskHelper.setProgressMaximum", name, maxSteps);
    call("net.imagej.legacy.task.TaskHelper.start", name);
}

function taskSetProgressValue(name, step) {
    call("net.imagej.legacy.task.TaskHelper.setProgressValue", taskName, step);
}

function taskFinish(name) {
    call("net.imagej.legacy.task.TaskHelper.finish", name);
}

function taskIsCanceled(name) {
    ans = call("net.imagej.legacy.task.TaskHelper.isCanceled", name);
    if (ans == "true") {
    	return 1;
    } else {
    	return 0;
    }
}
