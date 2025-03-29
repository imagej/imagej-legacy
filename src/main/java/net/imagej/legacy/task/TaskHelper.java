package net.imagej.legacy.task;

import net.imagej.legacy.LegacyService;
import org.scijava.log.LogService;
import org.scijava.object.ObjectService;
import org.scijava.task.Task;
import org.scijava.task.TaskService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Static methods enabling the use of {@link Task} in ImageJ macro language through the call method:
 * output = call('net.imagej.legacy.task.Taskhelper.METHODNAME', args, ...);
 * where METHODNAME belong to the {@link Task} interface
 * <p>
 * The ObjectService is used to store tasks created using this static class (within {@link TaskHelper#createTask(String)})
 * Tasks are removed from the ObjectService when {@link TaskHelper#finish(String)} is called.
 * <p>
 * Using this class, it is possible to create two tasks with the same name. Name uniqueness is not guaranteed.
 * If two tasks with the same name exist in the ObjectService, a warning message will be displayed via the
 * {@link LogService}. It is possible to clear all tasks present in the ObjectService with a specific name
 * using {@link TaskHelper#removeAll(String)}. This may be necessary to remove dangling tasks existing after
 * a macro has not not been completed successfully, or in the case where calling task.finish was forgotten.
 * <p>
 * The {@link LogService} and {@link ObjectService} are accessed
 * statically via the {@link LegacyService}, where a single instance can exist anyway.
 * <p>
 * @author Nicolas Chiaruttini, EPFL, 2023
 */
@SuppressWarnings("unused")
public class TaskHelper {

    // Since the LegacyService exists only once, the LogService and the ObjectService can be accessed statically
    private static volatile LogService logService;
    private static volatile ObjectService objectService;
    private static volatile TaskService taskService;
    public static LogService logService() {
        if (logService == null) {
            synchronized (LogService.class) {
                if (logService == null) {
                    logService = LegacyService.getInstance().log();
                }
            }
        }
        return logService;
    }

    public static ObjectService objectService() {
        if (objectService == null) {
            synchronized (ObjectService.class) {
                if (objectService == null) {
                    objectService = LegacyService.getInstance().context().getService(ObjectService.class);
                }
            }
        }
        return objectService;
    }

    public static TaskService taskService() {
        if (taskService == null) {
            synchronized (TaskService.class) {
                if (taskService == null) {
                    taskService = LegacyService.getInstance().context().getService(TaskService.class);
                }
            }
        }
        return taskService;
    }


    private static Task getTaskNamed(String name) throws IllegalArgumentException {

        List<Task> tasks = objectService().getObjects(Task.class)
                .stream()
                .filter(task -> task.getName().equals(name)).collect(Collectors.toList());

        if (tasks.size() == 0) {
            throw new IllegalArgumentException("No task named "+name+" was found in the object service.");
        }

        if (tasks.size() > 1) {
            logService().warn("Multiple tasks named "+name+" found! Taking the first one.");
        }

        return tasks.get(0);
    }

    private static List<Task> getTasksNamed(String name) {
        return objectService().getObjects(Task.class)
                .stream()
                .filter(task -> task.getName().equals(name)).collect(Collectors.toList());
    }

    /**
     * Returns true as a String if the (first) task named taskName found in the {@link ObjectService} has been cancelled
     * Returns false as a String if the (first) task named taskName found in the {@link ObjectService} has not been cancelled
     * @param taskName name of the task to act on
     * @return true or false as String depending on whether the task named taskName has been cancelled
     */
    public static String isCanceled(String taskName) {
        try {
            Task task = getTaskNamed(taskName);
            return task.isCanceled()?"true":"false";
        } catch (IllegalArgumentException e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
            return "Error: task not found. ("+e.getMessage()+")";
        }
    }

    /**
     * Returns {@link Task#getCancelReason()} on the (first) task named taskName found in the {@link ObjectService}
     * @param taskName name of the task to act on
     * @return the cancellation reason of the task named taskName
     */
    public static String getCancelReason(String taskName) {
        try {
            Task task = getTaskNamed(taskName);
            return task.getCancelReason();
        } catch (IllegalArgumentException e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
            return "Error: task not found. ("+e.getMessage()+")";
        }
    }

    /**
     * Creates a new {@link Task} named taskNamer and stores it into {@link ObjectService}
     * @param taskName name of the task to act on
     */
    public static void createTask(String taskName) {
        Task task = taskService().createTask(taskName);
        objectService().addObject(task);
    }

    /**
     * Sets the maximal progression of the first task {@link Task} named taskName found in the {@link ObjectService}
     * @param taskName name of the task to act on
     * @param max the progression maximum (when the task is done)
     */
    public static void setProgressMaximum(String taskName, String max) {
        try {
            Task task = getTaskNamed(taskName);
            task.setProgressMaximum(Long.parseLong(max));
        } catch (IllegalArgumentException e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
        }
    }

    /**
     * Returns {@link Task#getProgressMaximum()} on the (first) task named taskName found in the {@link ObjectService}
     * @param taskName name of the task to act on
     * @return the current maximal progression of the task named taskName
     */
    public static String getProgressMaximum(String taskName) {
        try {
            Task task = getTaskNamed(taskName);
            return Long.toString(task.getProgressMaximum());
        } catch (Exception e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
            return "task "+taskName+" not found.";
        }
    }

    /**
     * Sets the status message of the first task {@link Task} named taskName found in the {@link ObjectService}
     * @param taskName name of the task to act on
     * @param status the status message to set
     */
    public static void setStatusMessage(String taskName, String status) {
        try {
            Task task = getTaskNamed(taskName);
            task.setStatusMessage(status);
        } catch (Exception e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
        }
    }

    /**
     * Returns {@link Task#getStatusMessage()} on the (first) task named taskName found in the {@link ObjectService}
     * @param taskName name of the task to act on
     * @return the current status message of the task named taskName
     */
    public static String getStatusMessage(String taskName) {
        try {
            Task task = getTaskNamed(taskName);
            return task.getStatusMessage();
        } catch (Exception e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
            return "task "+taskName+" not found.";
        }
    }

    /**
     * Calls {@link Task#start()} on the (first) task named taskName found in the {@link ObjectService}
     * @param taskName name of the task to act on
     */
    public static void start(String taskName) {
        try {
            Task task = getTaskNamed(taskName);
            task.start();
        } catch (Exception e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
        }
    }

    /**
     * Calls {@link Task#setProgressValue(long)} on the (first) task named taskName found in the {@link ObjectService}
     * @param taskName name of the task to act on
     * @param step the current task progression value
     */
    public static void setProgressValue(String taskName, String step) {
        try {
            Task task = getTaskNamed(taskName);
            task.setProgressValue(Long.parseLong(step));
        } catch (Exception e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
        }
    }

    /**
     * Calls {@link Task#finish()} on the (first) task named taskName found in the {@link ObjectService}
     * @param taskName name of the task to act on
     */
    public static void finish(String taskName) {
        try {
            Task task = getTaskNamed(taskName);
            task.finish();
            objectService().removeObject(task);
        } catch (Exception e) {
            logService().error("Error: task not found. ("+e.getMessage()+")");
        }
    }

    /**
     * Clean up method to removes tasks dangling after a macro has crashed.
     * If several tasks exist with the same name, they will all be removed
     * @param taskName name of the tasks to remove
     */
    public static void removeAll(String taskName) {
        for (Task task: getTasksNamed(taskName)) {
            task.finish();
            objectService().removeObject(task);
        }
    }

}
