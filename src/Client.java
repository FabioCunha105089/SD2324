package src;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Client {
    private static DataInputStream in;
    private static DataOutputStream out;
    private static Menu menuHandler;
    private static String RESULT_PATH;
    private static boolean isLoggedIn = false;
    private static final ReentrantLock socketLock = new ReentrantLock();
    private static boolean exit = false;
    private static Status serverStatus = null;
    private static final ReentrantLock serverStatusLock = new ReentrantLock();
    private static final Condition updateStatus = serverStatusLock.newCondition();


    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("É necessário o caminho para a pasta onde serão guardados os ficheiros.");
            System.exit(0);
        }
        fixResultPath(args[0]);

        menuHandler = new Menu();

        try (Socket socket = new Socket("localhost", 9090)) {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.writeUTF(MessageTypes.NEW_CLIENT.typeToString());
            out.flush();
            if (validateUser(socket))
                startProgram();
            disconnect(socket);
        } catch (IOException e) {
            System.err.println("Erro de IO: " + e);
        }
    }

    private static boolean validateUser(Socket socket) throws IOException {
        while (!isLoggedIn) {
            menuHandler.displayInitialMenu();
            int choice = menuHandler.getUserChoice();
            if (choice == 0) {
                disconnect(socket);
                return false;
            }
            String[] credentials = menuHandler.getUserCredentials();
            MessageTypes type = MessageTypes.REGISTER;

            if (choice == 2)
                type = MessageTypes.LOGIN;

            boolean success = sendCredentials(type, credentials);

            if (success) {
                isLoggedIn = true;
                createListeningThread();
            }
        }
        return true;
    }

    private static void startProgram() throws IOException
    {
        while(!exit)
        {
            menuHandler.displayMainMenu();
            int choice = menuHandler.getUserChoice();

            if (exit) {
                System.err.println("Servidor crashou");
                break;
            }

            switch (choice)
            {
                case 1:
                    String filePath = menuHandler.getFilePath();
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
                    menuHandler.printStatus(serverStatus);
                    serverStatus = null;
                    serverStatusLock.unlock();
                    break;
                case 0:
                    exit = true;
                    break;
            }
        }
    }

    private static boolean sendCredentials(MessageTypes type, String[] credentials) throws IOException
    {
        String name = credentials[0];
        String pass = credentials[1];

        socketLock.lock();
        out.writeUTF(type.typeToString());
        out.writeUTF(name);
        out.writeUTF(pass);
        out.flush();
        socketLock.unlock();

        if (!in.readUTF().equals("0")) {
            switch (type) {
                case MessageTypes.REGISTER -> {
                    System.out.println("Registo falhou. Username já existe.");
                    return false;
                }
                case MessageTypes.LOGIN -> {
                    System.out.println("Login falhou. Credenciais erradas.");
                    return false;
                }
            }
        }
        return true;
    }

    private static void fixResultPath(String path) {
        RESULT_PATH = path;
        if (RESULT_PATH.charAt(RESULT_PATH.length() - 1) != '/')
            RESULT_PATH = RESULT_PATH.concat("/");
    }


    private static void disconnect(Socket socket) throws IOException {
        socket.shutdownOutput();
        socket.shutdownInput();
        socket.close();
        System.out.println("Desconectado.");
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
        }
    }

    private static void askStatus()
    {
        Status status = new Status();
        try {
            socketLock.lock();
            status.serialize(out);
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
                    String message = in.readUTF();
                    switch(MessageTypes.stringToType(message))
                    {
                        case STATUS:
                            serverStatusLock.lock();
                            serverStatus = Status.deserialize(in);
                            updateStatus.signal();
                            serverStatusLock.unlock();
                            break;
                        case TASK_SUCCESSFUL:
                            Task ts = Task.deserializeFromServer(in, MessageTypes.TASK_SUCCESSFUL);
                            ts.writeResultToFile(RESULT_PATH, MessageTypes.TASK_SUCCESSFUL);
                            break;
                        case TASK_FAILED:
                            Task tf = Task.deserializeFromServer(in, MessageTypes.TASK_FAILED);
                            tf.writeResultToFile(RESULT_PATH, MessageTypes.TASK_FAILED);
                            break;
                    }
                } catch (IOException e) {
                    exit = true;
                }
            }
        }, "ListeningThread").start();
    }
}
