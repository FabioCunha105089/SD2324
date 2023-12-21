package src;

import java.io.DataOutputStream;
import java.util.concurrent.locks.ReentrantLock;

public class ClientInfo {
    private final DataOutputStream out;
    private final ReentrantLock socketLock;

    public ClientInfo(DataOutputStream out, ReentrantLock socketLock) {
        this.out = out;
        this.socketLock = socketLock;
    }

    public DataOutputStream getOut() {
        return this.out;
    }

    public void lockSocket() {
        this.socketLock.lock();
    }

    public void unlockSocket() {
        this.socketLock.unlock();
    }
}
