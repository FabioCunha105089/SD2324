package src;

import java.io.*;
import java.net.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


public class Server {
    private static final int PORT = 9090;

    private static final ReentrantLock socketLock = new ReentrantLock();
    private static final Map<String, String> users = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor pronto para conexões na porta " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                     DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()))) {

                    String messageType = in.readUTF();

                    switch (MessageTypes.valueOf(messageType)) {
                        case LOGIN:
                            handleLogin(in, out);
                            break;

                        case REGISTER:
                            handleRegister(in, out);
                            break;

                        case TASK_REQUEST:
                            InetAddress clientAddress = clientSocket.getInetAddress();
                            int clientPort = clientSocket.getPort();
                            var cliente = clientAddress + ":" + clientPort;
                            Task task = Task.deserialize(in,MessageTypes.TASK_REQUEST);

                            processJob job = new processJob(cliente, task.getTask(), out, socketLock);
                            Thread thread = new Thread(job);
                            thread.start();
                            break;



                        default:
                            System.out.println("Mensagem desconhecida recebida: " + messageType);
                            break;
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao lidar com a conexão do cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }


    private static void handleLogin(DataInputStream in, DataOutputStream out) throws IOException {
        String username = in.readUTF();
        String password = in.readUTF();

        if (authenticateUser(username, password)) {
            out.writeUTF("Login realizado com sucesso!");
        } else {
            out.writeUTF("Falha no login. Credenciais inválidas.");
        }

        out.flush();
    }


    private static void handleRegister(DataInputStream in, DataOutputStream out) throws IOException {
        String username = in.readUTF();
        String password = in.readUTF();

        if (registerUser(username, password)) {
            out.writeUTF("Registro realizado com sucesso!");
        } else {
            out.writeUTF("Registro falhou. Usuário já existe.");
        }

        out.flush();
    }

    private static boolean authenticateUser(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    private static boolean registerUser(String username, String password) {
        if (!users.containsKey(username)) {
            users.put(username, password);
            return true;
        }
        return false;
    }
}