package src;

import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.locks.Condition;

public class WorkerInfo {
    private final int memory;
    private final Socket socket;
    private final Queue<Task> queue;
    private Condition cond;

    public WorkerInfo(int memory, Socket socket, Queue<Task> queue, Condition cond) {
        this.memory = memory;
        this.socket = socket;
        this.queue = queue;
        this.cond = cond;
    }

    public int getMemory() {
        return memory;
    }
    public Socket getSocket() {
        return socket;
    }
    public Queue<Task> getQueue() {
        return queue;
    }
    public Condition getCondition() { return cond; }
}
