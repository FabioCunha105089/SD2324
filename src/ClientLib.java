package src;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ClientLib {
    private static DataInputStream in;
    private static DataOutputStream out;
    private static String RESULT_PATH;
    private static Socket socket;
    private static final ReentrantLock socketLock = new ReentrantLock();
    private static boolean exit = false;
    private static Status serverStatus = null;
    private static final ReentrantLock serverStatusLock = new ReentrantLock();
    private static final Condition updateStatus = serverStatusLock.newCondition();
    private static boolean waitingForReply = false;
    private static final ReentrantLock waitingForReplyLock = new ReentrantLock();
    private static final Condition newMessageSent = waitingForReplyLock.newCondition();
    private static int tasksRequested = 0;
    private static final ReentrantLock tasksRequestedLock = new ReentrantLock();

    private ClientLib() {
    }
    public static void connectToServer() {
        try {
            socket = new Socket("localhost", 9090);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.writeUTF(MessageTypes.NEW_CLIENT.typeToString());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean validateUser(int choice, String[] credentials) {
        MessageTypes type = MessageTypes.REGISTER;
        String msg = "Registo";

        if (choice == 2) {
            type = MessageTypes.LOGIN;
            msg = "Login";
        }

        boolean success = sendCredentials(type, credentials);

        if (success) {
            System.out.println(msg + " efetuado com sucesso");
            createListeningThread();
            return true;
        }

        return false;
    }

    public static boolean startProgram(int choice, String filePath)
    {
        if (exit) {
            System.err.println("Servidor crashou");
            return true;
        }
        switch (choice)
        {
            case 1:
                try {
                    List<String[]> taskInfo = parseTaskInfo(filePath);
                    sendTask(taskInfo);
                } catch (FileNotFoundException e) {
                    System.out.println("ERRO: " + e);
                }
                break;
            case 2:
                askStatus();
                serverStatusLock.lock();
                if (serverStatus == null) {
                    try {
                        updateStatus.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                printStatus(serverStatus);
                serverStatus = null;
                serverStatusLock.unlock();
                break;
            case 0:
                exit = true;
                break;
            default:
                break;
        }
        return exit;
    }

    private static void printStatus(Status status) {
        System.out.println("Tarefas na fila de espera: " + status.getQueueSize());
        if (status.getWorkersInfo() != null) {
            for (String info : status.getWorkersInfo())
                System.out.println(info);
        }
    }

    private static boolean sendCredentials(MessageTypes type, String[] credentials)
    {
        String name = credentials[0];
        String pass = credentials[1];
        try {
            socketLock.lock();
            out.writeUTF(type.typeToString());
            out.writeUTF(name);
            out.writeUTF(pass);
            out.flush();
            socketLock.unlock();

            if (!in.readUTF().equals("0")) {
                switch (type) {
                    case MessageTypes.REGISTER:
                        System.out.println("Registo falhou. Username já existe.");
                        return false;
                    case MessageTypes.LOGIN:
                        System.out.println("Login falhou. Credenciais erradas.");
                        return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void fixResultPath(String path) {
        RESULT_PATH = path;
        if (RESULT_PATH.charAt(RESULT_PATH.length() - 1) != '/')
            RESULT_PATH = RESULT_PATH.concat("/");
    }

    public static void disconnect(){
        try {
            socket.shutdownOutput();
            socket.shutdownInput();
            socket.close();
            System.out.println("Desconectado.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendTask(List<String[]> taskInfo) {
        for (String[] values : taskInfo) {
            String taskName = values[0];
            int mem = Integer.parseInt(values[1]);
            byte[] job = values[2].getBytes();
            Task task = new Task(taskName, mem, job);
            try {
                socketLock.lock();
                task.serialize(out);
            } catch (IOException e) {
                System.out.println("ERRO: " + e);
            } finally {
                socketLock.unlock();
            }
            updateTasksRequested(MessageTypes.TASK_REQUEST);
        }
    }

    private static void askStatus()
    {
        Status status = new Status();
        try {
            socketLock.lock();
            status.serialize(out);
            waitingForReplyLock.lock();
            if (!waitingForReply) {
                waitingForReply = true;
                newMessageSent.signal();
            }
            waitingForReplyLock.unlock();
        } catch (IOException e) {
            System.out.println("ERRO: " + e);
        } finally {
            socketLock.unlock();
        }
    }

    private static List<String[]> parseTaskInfo(String filePath) throws FileNotFoundException {
        List<String[]> tasks = new ArrayList<>();

        try(Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] values = line.split(";");
                tasks.add(values);
            }
        }
        return tasks;
    }

    private static void createListeningThread ()
    {
        new Thread(() ->
        {
            while(!exit)
            {
                try{
                    waitingForReplyLock.lock();
                    if (!waitingForReply) {
                        newMessageSent.await();
                    }
                    waitingForReplyLock.unlock();

                    String message = in.readUTF();
                    MessageTypes type = MessageTypes.stringToType(message);
                    switch(type)
                    {
                        case STATUS:
                            serverStatusLock.lock();
                            serverStatus = Status.deserialize(in);
                            updateStatus.signal();
                            serverStatusLock.unlock();
                            updateTasksRequested(type);
                            break;
                        case TASK_SUCCESSFUL:
                            Task ts = Task.deserializeFromServer(in, MessageTypes.TASK_SUCCESSFUL);
                            ts.writeResultToFile(RESULT_PATH, MessageTypes.TASK_SUCCESSFUL);
                            updateTasksRequested(type);
                            break;
                        case TASK_FAILED:
                            Task tf = Task.deserializeFromServer(in, MessageTypes.TASK_FAILED);
                            tf.writeResultToFile(RESULT_PATH, MessageTypes.TASK_FAILED);
                            updateTasksRequested(type);
                            break;
                    }
                } catch (IOException e) {
                    exit = true;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "ListeningThread").start();
    }

    private static void updateTasksRequested(MessageTypes type) {
        tasksRequestedLock.lock();
        if (type.equals(MessageTypes.TASK_REQUEST)) {
            tasksRequested++;
            waitingForReplyLock.lock();
            if (!waitingForReply)
                waitingForReply = true;
            newMessageSent.signal();
            waitingForReplyLock.unlock();
        }else {
            if (!type.equals(MessageTypes.STATUS))
                tasksRequested--;

            if (tasksRequested == 0) {
                waitingForReplyLock.lock();
                waitingForReply = false;
                waitingForReplyLock.unlock();
            }
        }
        tasksRequestedLock.unlock();
    }
}
