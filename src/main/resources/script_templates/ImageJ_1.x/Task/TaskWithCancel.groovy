#@ TaskService  taskService
#@ Double (label = "Waiting time per step (s)", style = "0.000", value = "0.5") wait_s
#@ Integer(label = "Number of steps", value = "20") n_steps

Task task = taskService.createTask("A task");
task.setProgressMaximum(n_steps)

try {

	task.start()
	//... do stuff
	for (int i = 0; i<n_steps; i++) {
		task.setProgressValue(i); // Updates the progress bar with the current progression
		Thread.sleep((int) (wait_s*1000))
		if (task.isCanceled()) {
			IJ.log(task.getName()+" has been cancelled");
			return ; // the finally statement will be executed anyway, no need to finish the task
		}
	}

} finally {
	// Block always executed, irrespective of a previous potential crash
	task.finish()
}


import ij.IJ
import org.scijava.task.Task
import org.scijava.task.TaskService
