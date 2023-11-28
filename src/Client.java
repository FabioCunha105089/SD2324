package src;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    //Receber no socket
    private static DataInputStream in;
    //Enviar no socket
    private static DataOutputStream out;
    //Ler do teclado
    private static BufferedReader sysIn;
    private static String RESULT_PATH;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("É necessário o caminho para a pasta onde serão guardados os ficheiros.");
            System.exit(0);
        }
        fixResultPath(args[0]);
        System.out.print(RESULT_PATH);
        try (Socket socket = new Socket("localhost", 9090)) {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            sysIn = new BufferedReader(new InputStreamReader(System.in));
            startProgram();
            createListeningThread();
            System.out.println("Bem-vindo");
            boolean exit = false;
            while (!exit) {
                System.out.println("""
                        Escolha uma opção:
                        1- Enviar tarefa
                        2- Verificar estado
                        0- Sair""");
                String input = sysIn.readLine();
                if (!isDigit(input)) {
                    System.out.println("Insira um valor válido.");
                    continue;
                }
                int choice = Integer.parseInt(input);
                switch (choice) {
                    case 1:
                        String mem = readMemory();
                        File file = readFile();
                        String task = getTaskFromFile(file);
                        sendTask(mem, task, file.getPath());
                        break;
                    case 2:
                        askStatus();
                        break;

                    case 0:
                        disconnect(socket);
                        exit = true;
                        break;

                    default:
                        System.out.println("Insira um valor válido.");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Erro de IO: " + e);
        }
    }

    private static void startProgram() throws IOException
    {
        while(true)
        {
            System.out.println("""
                        Escolha uma opção:
                        1- Registar
                        2- Login
                        0- Sair""");
            String input = sysIn.readLine();
            if (!isDigit(input)) {
                System.out.println("Insira um valor válido.");
                continue;
            }
            switch (Integer.parseInt(input))
            {
                case 1:
                    sendCredentials(MessageTypes.REGISTER, "Registro falhou. Usuário já existe.");
                    break;
                case 2:
                    sendCredentials(MessageTypes.LOGIN, "Credenciais erradas, tente denovo.");
                    break;
                case 3:
                    System.exit(0);
            }
        }
    }

    private static void sendCredentials(MessageTypes type, String errorMsg) throws IOException
    {
        while (true) {
            System.out.println("Insira nome: ");
            String name = sysIn.readLine();
            System.out.println("Insira password: ");
            String pass = sysIn.readLine();

            out.writeUTF(type.typeToString());
            out.writeUTF(name);
            out.writeUTF(pass);
            out.flush();
            if (in.readUTF().equals(errorMsg)) {
                System.out.println(errorMsg);
                continue;
            }
            break;
        }
    }

    private static void fixResultPath(String path) {
        RESULT_PATH = path;
        if (RESULT_PATH.charAt(RESULT_PATH.length() - 1) != '/')
            RESULT_PATH = RESULT_PATH.concat("/");
    }

    private static boolean isDigit(String input) {
        if (input == null || input.isBlank() || input.isEmpty())
            return false;
        for (char c : input.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }

    private static void disconnect(Socket socket) throws IOException {
        socket.shutdownOutput();
        socket.shutdownInput();
        socket.close();
        System.out.println("Disconectado.");
    }

    private static void sendTask(String mem, String task, String path) throws IOException {
        new Task(path, Integer.parseInt(mem), task.getBytes()).serialize(out);
    }

    private static String readMemory() throws IOException
    {
        String mem;
        while(true) {
            System.out.println("Quanta memória vai precisar?:");
            mem = sysIn.readLine();
            if (isDigit(mem))
                break;
            System.out.println("Insira um valor válido.");
        }
        return mem;
    }

    private static void askStatus() throws IOException
    {
        new Status().serialize(out);
    }

    private static File readFile() throws IOException
    {
        String path;
        File file;
        while(true) {
            System.out.println("Insira o caminho para o ficheiro com a tarefa:");
            path = sysIn.readLine();
            try {
                file = new File(path);
                break;
            } catch (Exception e) {
                System.out.println("Exception a ler ficheiro: " + e);
            }
        }
        return file;
    }

    private static String getTaskFromFile(File file) throws FileNotFoundException
    {
        Scanner scanner = new Scanner(file);
        return scanner.nextLine();
    }

    private static void createListeningThread()
    {
        new Thread(() ->
        {
            while(true)
            {
                try{
                    String message = in.readUTF();
                    switch(MessageTypes.stringToType(message))
                    {
                        case STATUS:
                            Status s = Status.deserialize(in);
                            s.printStatus();
                            break;
                        case TASK_SUCCESSFUL:
                            Task ts = Task.deserialize(in, MessageTypes.TASK_SUCCESSFUL);
                            ts.writeResultToFile(RESULT_PATH);
                            break;
                        case TASK_FAILED:
                            Task tf = Task.deserialize(in, MessageTypes.TASK_FAILED);
                            tf.printError();
                            break;
                    }
                }
                catch(IOException e)
                {
                    System.out.println("Erro a ler mensagem do servidor: " + e);
                }
            }
        }).start();
    }
}
