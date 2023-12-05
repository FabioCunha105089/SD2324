package src;

import java.io.*;
import java.net.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


public class Server {
    private static final int PORT = 9090;
    private static final Map<String, String> users = new HashMap<>();
    private static final Map<String, WorkerInfo> workers = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor pronto para conexões na porta " + PORT);
            acceptClients(serverSocket);
            acceptWorkers(serverSocket);
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

    private static void acceptClients(ServerSocket serverSocket)
    {
        new Thread(() -> {
            while (true) {
                Socket clientSocket;
                try {
                    clientSocket = serverSocket.accept();
                } catch (IOException e) {
                    System.out.println("Erro a conectar a cliente: " + e);
                    continue;
                }
                new Thread(() -> {
                    try {
                        DataInputStream in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
                        String messageType = in.readUTF();
                        switch (MessageTypes.valueOf(messageType)) {
                            case LOGIN:
                                handleLogin(in, out);
                                break;

                            case REGISTER:
                                handleRegister(in, out);
                                break;
                            case STATUS:
                                int tMemory = 0;
                                int tQueue = 0;
                                for (WorkerInfo info:
                                        workers.values()) {
                                    tMemory += info.getMemory();
                                    tQueue += info.getQueue().size();
                                }
                                new Status(tMemory, tQueue).serialize(out);
                                break;
                            case TASK_REQUEST:
                                InetAddress clientAddress = clientSocket.getInetAddress();
                                int clientPort = clientSocket.getPort();
                                var cliente = clientAddress + ":" + clientPort;
                                Task task = Task.deserialize(in, MessageTypes.TASK_REQUEST);
                                // TODO: escolher um worker e enviar a task
                                        /*
                                        outS.writeUTF(cliente);
                                        outS.writeInt(task.getMem());
                                        outS.write(task.getTask());

                                        var message = inS.readUTF();
                                        if (MessageTypes.valueOf(message) == MessageTypes.TASK_SUCCESSFUL) {
                                            out.writeUTF(inS.readUTF()); // cliente
                                            out.write(inS.read()); // output
                                        } else if (MessageTypes.valueOf(message) == MessageTypes.TASK_FAILED) {
                                            out.writeInt(inS.readInt()); // error code
                                            out.writeUTF(inS.readUTF()); // error message
                                        }
                                         */
                                break;
                        }
                    } catch (IOException e) {
                        System.out.println("Erro a comunicar com cliente: " + e);
                    }
                }).start();
            }
        }).start();
    }

    private static void acceptWorkers(ServerSocket serverSocket)
    {
        while (true) {
            try {
                Socket workerSocket = serverSocket.accept();
                DataInputStream inS = new DataInputStream(new BufferedInputStream(workerSocket.getInputStream()));
                DataOutputStream outS = new DataOutputStream(new BufferedOutputStream(workerSocket.getOutputStream()));

                new Thread(() -> {
                    int memory = 0;
                    String worker = "";
                    try {
                        String newWorker = inS.readUTF();
                        var workerIP = workerSocket.getInetAddress();
                        var workerPort = workerSocket.getPort();
                        worker = workerIP + ":" + workerPort;
                        memory = inS.readInt();
                    } catch (IOException e) {
                        System.out.println("Erro a ler memória do worker: " + e);
                    }
                    if (!workers.containsKey(worker)) {
                        workers.put(worker, new WorkerInfo(memory, workerSocket, new ArrayDeque<>()));
                    } else {
                        System.out.println("Erro ao inserir");
                    }
                }).start();
            } catch (IOException e) {
                System.err.println("Erro ao lidar com a conexão do cliente: " + e.getMessage());
            }
        }
    }
}