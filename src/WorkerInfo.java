package src;

import java.io.DataOutputStream;
import java.util.LinkedList;
import java.util.Queue;

public class WorkerInfo {
    private final int memory;
    private int availableMemory;
    private final DataOutputStream out;
    private final Queue<Task> queue;

    public WorkerInfo(int memory, DataOutputStream out, Queue<Task> queue) {
        this.memory = memory;
        this.availableMemory = memory;
        this.out = out;
        this.queue = queue;
    }

    public int getMemory() {
        return memory;
    }
    public DataOutputStream getOut() {
        return out;
    }

    public void addTasktoQueue(Task task) {
        this.queue.add(task);
    }
    public void removeTaskFromQueue(String taskId) {
        Queue<Task> tempQueue = new LinkedList<>();

        for (Task currentTask : this.queue) {
            if(!currentTask.getTaskId().equals(taskId)) {
                tempQueue.add(currentTask);
            }
        }

        this.queue.clear();
        this.queue.addAll(tempQueue);
    }
    public int getAvailableMemory() {
        return availableMemory;
    }
    public void useMemory(int mem) {
        this.availableMemory -= mem;
    }
    public void freeMemory(int mem) {
        this.availableMemory += mem;
    }
    public int getTaskMem(String taskId) {
        for (Task currentTask : this.queue) {
            if (currentTask.getTaskId().equals(taskId)) {
                return currentTask.getMem();
            }
        }
        return 0;
    }
    public Queue<Task> getQueue() {
        return this.queue;
    }
}
