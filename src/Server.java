package src;

import java.io.*;
import java.net.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class Server {
    private static final int PORT = 9090;
    private static final Map<String, String> users = new HashMap<>();
    private static final Map<String, ClientInfo> clients = new HashMap<>();
    private static final Map<String, WorkerInfo> workers = new HashMap<>();
    private static final ReentrantLock clientsLock = new ReentrantLock();
    private static final ReentrantLock workersLock = new ReentrantLock();
    private static final Condition memoryFreed = workersLock.newCondition();
    private static final Queue<Task> taskQueue = new ArrayDeque<>();
    private static final ReentrantLock taskQueueLock = new ReentrantLock();
    private static final Condition queueNotEmpty = taskQueueLock.newCondition();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor pronto para conexões na porta " + PORT);
            taskDistributionThread();
            while (true) {
                Socket newSocket = serverSocket.accept();
                DataInputStream in = new DataInputStream(new BufferedInputStream(newSocket.getInputStream()));
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(newSocket.getOutputStream()));
                String newConnection = in.readUTF();
                switch (MessageTypes.stringToType(newConnection)) {
                    case NEW_WORKER:
                        workerThread(newSocket, in, out);
                        break;
                    case NEW_CLIENT:
                        clientThread(newSocket, in, out);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("ERRO: " + e);
        }
    }

    private static void taskDistributionThread() {
        new Thread(() ->{
            while (true) {
                taskQueueLock.lock();
                try {
                    while (taskQueue.isEmpty()) {
                        try {
                            queueNotEmpty.await();
                        } catch (InterruptedException e) {
                            System.err.println("Thread interrupted: " + e.getMessage());
                        }
                    }

                    Task task = taskQueue.poll();
                    taskQueueLock.unlock();
                    clientsLock.lock();
                    if (task != null && clients.containsKey(task.getClient())) {
                        clientsLock.unlock();
                        boolean processed = false;
                        while (!processed) {
                            workersLock.lock();
                            WorkerInfo worker = getWorkerWithMostMemory();
                            if (worker.getAvailableMemory() < task.getMem()) {
                                taskQueueLock.lock();
                                //Envia a próxima task na queue caso haja memória suficiente. Depois espera até ter
                                // haver memória para a primeira. Só vê a próxima para evitar que a primeira fique à
                                // para sempre
                                if (!taskQueue.isEmpty()) {
                                    Task nextTask = taskQueue.peek();
                                    clientsLock.lock();
                                    if(clients.containsKey(task.getClient()) && nextTask.getMem() <= worker.getAvailableMemory()) {
                                        clientsLock.unlock();
                                        nextTask = taskQueue.poll();
                                        processTask(worker, nextTask);
                                    }else {
                                        clientsLock.unlock();
                                    }
                                }
                                taskQueueLock.unlock();
                                memoryFreed.await();
                                workersLock.unlock();
                            } else {
                                processTask(worker, task);
                                workersLock.unlock();
                                processed = true;
                            }
                        }
                    }else {
                        clientsLock.unlock();
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao enviar para worker: " + e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Erro ao esperar por memory freed: " + e.getMessage());
                }
            }
        }, "TaskDistributionThread").start();
    }

    private static void processTask(WorkerInfo worker, Task task) throws IOException {
        worker.addTasktoQueue(task);
        worker.useMemory(task.getMem());
        task.serializeToWorker(worker.getOut());
    }

    private static void handleLogin(DataInputStream in, DataOutputStream out) throws IOException {
        String username = in.readUTF();
        String password = in.readUTF();

        if (authenticateUser(username, password)) {
            out.writeUTF("0");
        } else {
            out.writeUTF("1");
        }
        out.flush();
    }


    private static void handleRegister(DataInputStream in, DataOutputStream out) throws IOException {
        String username = in.readUTF();
        String password = in.readUTF();

        if (registerUser(username, password)) {
            out.writeUTF("0");
        } else {
            out.writeUTF("1");
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

    private static void clientThread(Socket clientSocket, DataInputStream in, DataOutputStream out){
        new Thread(() -> {
            InetAddress clientAddress = clientSocket.getInetAddress();
            int clientPort = clientSocket.getPort();
            var cliente = clientAddress + ":" + clientPort;
            ReentrantLock socketLock = new ReentrantLock();
            clientsLock.lock();
            clients.put(cliente, new ClientInfo(out, socketLock));
            clientsLock.unlock();

            try {
                while (true) {
                    String messageType = in.readUTF();
                    switch (MessageTypes.stringToType(messageType)) {
                        case LOGIN:
                            handleLogin(in, out);
                            break;
                        case REGISTER:
                            handleRegister(in, out);
                            break;
                        case STATUS:
                            getServerStatus().serialize(out);
                            break;
                        case TASK_REQUEST:
                            Task task = Task.deserialize(in, cliente);
                            taskQueueLock.lock();
                            try {
                                taskQueue.add(task);
                                queueNotEmpty.signal();
                            } finally {
                                taskQueueLock.unlock();
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                try {
                    clientSocket.shutdownOutput();
                    clientSocket.shutdownInput();
                    clientSocket.close();
                    clientsLock.lock();
                    clients.remove(cliente);
                    clientsLock.unlock();
                    System.out.println("Cliente desconetado");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, "ClientThread").start();
    }

    private static Status getServerStatus() {
        List<String> workersInfo = new ArrayList<>();
        int i = 1;
        taskQueueLock.lock();
        int queueSize = taskQueue.size();
        taskQueueLock.unlock();
        workersLock.lock();
        for (WorkerInfo workerInfo : workers.values()) {

            int totalMemory = workerInfo.getMemory();
            int availableMemory = workerInfo.getAvailableMemory();

            int workerLoad = (int) (((double) (totalMemory - availableMemory) / totalMemory) * 100);

            workersInfo.add("Worker " + i + " -> Carga: " + workerLoad + "%");
            i++;
        }
        workersLock.unlock();

        return new Status(queueSize, workersInfo);
    }

    public static WorkerInfo getWorkerWithMostMemory() {
        int maxMemory = Integer.MIN_VALUE;
        WorkerInfo maxMemoryWorker = null;
        for (Map.Entry<String, WorkerInfo> entry : workers.entrySet()) {
            WorkerInfo worker = entry.getValue();

            int availableMemory = worker.getAvailableMemory();
            if (availableMemory > maxMemory) {
                maxMemory = availableMemory;
                maxMemoryWorker = worker;
            }
        }
        return maxMemoryWorker;
    }

    private static void workerThread(Socket workerSocket, DataInputStream in, DataOutputStream out)
    {
        new Thread(() -> {
            int memory = 0;
            String worker = "";
            Queue<Task> queue = new ArrayDeque<>();
            try {
                var workerIP = workerSocket.getInetAddress();
                var workerPort = workerSocket.getPort();
                worker = workerIP + ":" + workerPort;
                memory = in.readInt();
            } catch (IOException e) {
                System.out.println("Erro a ler memória do worker: " + e);
            }
            if (!workers.containsKey(worker)) {
                workers.put(worker, new WorkerInfo(memory, out, queue));
            } else {
                System.out.println("Erro ao inserir");
            }

            try {
                while (true) {
                    String messageType = in.readUTF();
                    MessageTypes type = MessageTypes.stringToType(messageType);

                    Task task = Task.deserialize(in,type);
                    clientsLock.lock();
                    if (clients.containsKey(task.getClient())) {
                        clientsLock.unlock();
                        sendResultToClient(task, type);
                    }else {
                        clientsLock.unlock();
                    }

                    updateWorkerInfo(worker, task.getTaskId());
                }
            } catch (IOException e) {
                try {
                    workerSocket.shutdownOutput();
                    workerSocket.shutdownInput();
                    workerSocket.close();
                    workersLock.lock();
                    taskQueueLock.lock();
                    taskQueue.addAll(workers.get(worker).getQueue());
                    taskQueueLock.unlock();
                    workers.remove(worker);
                    workersLock.unlock();
                    System.out.println("Worker desconetado.");
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, "WorkerThread").start();
    }

    private static void updateWorkerInfo(String worker, String taskId) {
        workersLock.lock();
        WorkerInfo workerInfo = workers.get(worker);
        int tsMem = workerInfo.getTaskMem(taskId);
        workerInfo.freeMemory(tsMem);
        workerInfo.removeTaskFromQueue(taskId);
        memoryFreed.signal();
        workersLock.unlock();
    }

    private static void sendResultToClient(Task task, MessageTypes type) throws IOException {
        String client = task.getClient();
        clientsLock.lock();
        clients.get(client).lockSocket();
        task.serializeToClient(clients.get(client).getOut(), type);
        clients.get(client).unlockSocket();
        clientsLock.unlock();
    }
}