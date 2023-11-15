package src;
import sd23.*;
import java.io.*;
import java.net.Socket;

class processJob implements Runnable {
    private final String client;
    private final byte[] job;
    private final DataOutputStream toServer;

    public processJob(String client, byte[] job, DataOutputStream toServer) {
        this.client = client;
        this.job = job;
        this.toServer = toServer;
    }

    @Override
    public void run() {

        try {
            byte[] output = JobFunction.execute(job);
            toServer.writeUTF(MessageTypes.TASK_SUCCESSFUL.typeToString());
            toServer.writeUTF(client);
            toServer.write(output);

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
    private final int memory = 100;

    public static void main(String[] args) throws IOException {

        try (Socket socket = new Socket("localhost", 9090);
             DataInputStream fromServer = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream toServer = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            System.out.println("Conetou ao servidor central.");

            while (true) {
                try {
                    String client = fromServer.readUTF();
                    byte[] job = fromServer.readAllBytes();
                    new Thread(new processJob(client, job, toServer)).start();
                }catch (EOFException eof) {
                    System.out.println("Conexão encerrada. A sair...");
                    break;
                }
            }
        }
    }
}

