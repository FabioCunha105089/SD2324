package src;
import sd23.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

class processJob implements Runnable {
    private final String client;
    private final String taskId;
    private final byte[] job;
    private final DataOutputStream toServer;
    private final ReentrantLock socketLock;

    public processJob(String client, String taskId, byte[] job, DataOutputStream toServer, ReentrantLock socketLock) {
        this.client = client;
        this.taskId = taskId;
        this.job = job;
        this.toServer = toServer;
        this.socketLock = socketLock;
    }

    @Override
    public void run() {
        try {
            byte[] output = JobFunction.execute(job);
            System.out.println("Executei " + this.taskId);

            try {
                socketLock.lock();
                toServer.writeUTF(MessageTypes.TASK_SUCCESSFUL.typeToString());
                toServer.writeUTF(client);
                toServer.writeUTF(taskId);
                toServer.writeInt(output.length);
                toServer.write(output);
                toServer.flush();
            }catch (IOException i) {
                System.err.println("IOException: " + i);
            }finally {
                socketLock.unlock();
            }

        }catch (JobFunctionException e) {
            System.err.println(e.getMessage());
            try {
                socketLock.lock();
                toServer.writeUTF(MessageTypes.TASK_FAILED.typeToString());
                toServer.writeUTF(client);
                toServer.writeUTF(taskId);
                toServer.writeInt(e.getCode());
                toServer.writeUTF(e.getMessage());
                toServer.flush();
            } catch (IOException i) {
                System.err.println("IOException: " + i);
            } finally {
                socketLock.unlock();
            }
        }
    }
}

public class Worker {
    private static final ReentrantLock socketLock = new ReentrantLock();

    public static void main(String[] args) {

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
            toServer.flush();

            System.out.println("Conetou ao servidor central.");

            while (true) {
                try {
                    String client = fromServer.readUTF();
                    String taskId = fromServer.readUTF();
                    int taskLength = fromServer.readInt();
                    byte[] job = fromServer.readNBytes(taskLength);
                    new Thread(new processJob(client, taskId, job, toServer, socketLock)).start();
                }catch (EOFException eof) {
                    System.out.println("Conexão encerrada. A sair...");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }
}