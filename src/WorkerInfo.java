package src;

import java.io.DataOutputStream;
import java.util.Map;

public class WorkerInfo {
    private final int memory;
    private int availableMemory;
    private final DataOutputStream out;
    private final Map<String, Task> queue;

    public WorkerInfo(int memory, DataOutputStream out, Map<String, Task> queue) {
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

    public void addTasktoQueue(String taskId, Task task) {
        this.queue.put(taskId, task);
    }
    public void removeTaskFromQueue(String taskId) {
        this.queue.remove(taskId);
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
        if (this.queue.containsKey(taskId))
            return this.queue.get(taskId).getMem();
        return 0;
    }
    public Map<String, Task> getQueue() {
        return this.queue;
    }
}
