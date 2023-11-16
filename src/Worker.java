package src;
import sd23.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

class processJob implements Runnable {
    private final String client;
    private final byte[] job;
    private final DataOutputStream toServer;
    private final ReentrantLock socketLock;

    public processJob(String client, byte[] job, DataOutputStream toServer, ReentrantLock socketLock) {
        this.client = client;
        this.job = job;
        this.toServer = toServer;
        this.socketLock = socketLock;
    }

    @Override
    public void run() {
        socketLock.lock();
        try {
            byte[] output = JobFunction.execute(job);

            socketLock.lock();
            try {
                toServer.writeUTF(MessageTypes.TASK_SUCCESSFUL.typeToString());
                toServer.writeUTF(client);
                toServer.write(output);
            }finally {
                socketLock.unlock();
            }

        }catch (JobFunctionException e) {
            try {
                toServer.writeUTF(MessageTypes.TASK_FAILED.typeToString());
                toServer.writeInt(e.getCode());
                toServer.writeUTF(e.getMessage());
            }catch (IOException i) {
                System.err.println("IOException: " + i);
            }
        }catch (IOException e) {
            System.err.println("IOException: " + e);
        }
    }
}

public class Worker {
    private static final ReentrantLock socketLock = new ReentrantLock();

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.err.println("Comando correto: java Worker <memory>");
            System.exit(1);
        }

        int memory = Integer.parseInt(args[0]);


        try (Socket socket = new Socket("localhost", 9090);
             DataInputStream fromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream toServer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            toServer.writeUTF(MessageTypes.NEW_WORKER.typeToString());
            toServer.writeInt(memory);

            System.out.println("Conetou ao servidor central.");

            while (true) {
                try {
                    String client = fromServer.readUTF();
                    byte[] job = fromServer.readAllBytes();
                    new Thread(new processJob(client, job, toServer, socketLock)).start();
                }catch (EOFException eof) {
                    System.out.println("Conexão encerrada. A sair...");
                    break;
                }
            }
        }
    }
}

