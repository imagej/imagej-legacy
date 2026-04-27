#@ TaskService  taskService
#@ Double (label = "Waiting time per step (s)", style = "0.000", value = "0.5") wait_s
#@ Integer (label = "Number of steps", value = "20") n_steps
#@ boolean(label = "Wait for end of task? ") demo_wait_for_end_of_task
#@ boolean(label = "Cancel programmatically the task ") demo_cancel

Task task = taskService.createTask("A task");
task.setProgressMaximum(n_steps)

task.run {
	//... do stuff
	for (int i = 0; i<n_steps; i++) {
		try {
			task.setProgressValue(i); // Updates the progress bar with the current progression
			Thread.sleep((int) (wait_s*1000))
		} catch (InterruptedException e) {
			IJ.log("The task has been interrupted");
			if (task.isCanceled()) {
				IJ.log("The task has been canceled: "+task.getCancelReason())
				return
			}
		}
		IJ.log("i="+i);
		if (task.isCanceled()) {
			IJ.log("The task has been canceled: "+task.getCancelReason())
			return
		}
	}
}

if (demo_cancel) {
	Task cancelTask = taskService.createTask("Cancel a task")
	cancelTask.run {
		IJ.log("wait")
		Thread.sleep(3000)
		IJ.log("cancel")
		task.cancel("Programmatic cancellation")
		IJ.log("done")
	}
}

if (demo_wait_for_end_of_task) {
	try {
		task.waitFor();
		if (task.isDone()) {
			IJ.log("The task is done");
		}
	} catch (CancellationException e) {
		if (task.isCanceled()) {
			IJ.log("The task has been canceled");
		} else {
			// Should not happen
			IJ.log("The task has been canceled but the flag has not been set correctly");
		}
	}
}

import ij.IJ
import org.scijava.task.Task
import org.scijava.task.TaskService

import java.util.concurrent.CancellationException
