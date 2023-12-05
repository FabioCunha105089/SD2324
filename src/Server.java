package src;

import java.io.*;
import java.net.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class Server {
    private static final int PORT = 9090;
    private static final Map<String, String> users = new HashMap<>();
    private static final Map<String, WorkerInfo> workers = new HashMap<>();
    private static ReentrantLock queueLock = new ReentrantLock();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor pronto para conexões na porta " + PORT);
            while (true) {
                Socket connectedSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        DataInputStream in = new DataInputStream(new BufferedInputStream(connectedSocket.getInputStream()));
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(connectedSocket.getOutputStream()));
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
                                for (WorkerInfo info :
                                        workers.values()) {
                                    tMemory += info.getMemory();
                                    tQueue += info.getQueue().size();
                                }
                                new Status(tMemory, tQueue).serialize(out);
                                break;
                            case TASK_REQUEST:
                                InetAddress clientAddress = connectedSocket.getInetAddress();
                                int clientPort = connectedSocket.getPort();
                                var cliente = clientAddress + ":" + clientPort;
                                Task task = Task.deserialize(in, MessageTypes.TASK_REQUEST);
                                // TODO: escolher um worker e enviar a task. Colocar Task na queue dela no dicionário
                                // workers. Dentro desse dicionário tem também uma Condition, usar signal() para
                                // acordar o worker. Utilizar queueLock antes de adicionar à queue
                                break;
                            case NEW_WORKER:
                                workerProcedure(connectedSocket, in, out);
                                break;
                        }
                    } catch (IOException e) {
                        System.out.println("Erro a comunicar com cliente: " + e);
                    }
                }).start();
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

    private static void workerProcedure(Socket connectedSocket, DataInputStream in, DataOutputStream out)
    {
        new Thread(() -> {
            int memory = 0;
            String worker = "";
            ReentrantLock lock = new ReentrantLock();
            Condition cond = lock.newCondition();
            Queue<Task> queue = new ArrayDeque<>();
            try {
                var workerIP = connectedSocket.getInetAddress();
                var workerPort = connectedSocket.getPort();
                worker = workerIP + ":" + workerPort;
                memory = in.readInt();
            } catch (IOException e) {
                System.out.println("Erro a ler memória do worker: " + e);
            }
            if (!workers.containsKey(worker)) {
                workers.put(worker, new WorkerInfo(memory, connectedSocket, queue, cond));
            } else {
                System.out.println("Erro ao inserir");
            }
            while (true) {
                while (queue.isEmpty()) {
                    try {
                        cond.await();
                    } catch (InterruptedException e) {
                        System.out.println("Erro enquanto espera por task para worker: " + e);
                    }
                }
                try {
                    out.writeUTF("NÃO SEI O QUE ELE MANDA AQUI");
                    byte[] task;
                    queueLock.lock();
                    task = queue.element().getTask();
                    queueLock.unlock();
                    out.write(task);
                    String message = in.readUTF();
                    if (MessageTypes.stringToType(message) == MessageTypes.TASK_SUCCESSFUL) {
                        String client = in.readUTF();
                        byte[] result = in.readAllBytes();
                        // TODO: Enviar resposta ao cliente
                    } else {
                        // TODO: Enviar erro ao cliente
                    }
                } catch (IOException e) {
                    System.out.println("Erro a enviar task a worker: " + e);
                }
            }
        }).start();
    }
}