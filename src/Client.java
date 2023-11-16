package src;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client
{
    //Receber no socket
    private static DataInputStream in;
    //Enviar no socket
    private static DataOutputStream out;
    //Ler do teclado
    private static BufferedReader sysIn;
    private static String RESULT_PATH;
    public static void main(String[] args)
    {
        if(args.length != 1)
        {
            System.out.println("É necessário o caminho para a pasta onde serão guardados os ficheiros.");
            System.exit(0);
        }
        fixResultPath(args[0]);
        System.out.print(RESULT_PATH);
        try (Socket socket = new Socket("localhost", 9090)){
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            sysIn = new BufferedReader(new InputStreamReader(System.in));
            login();
            System.out.println("Bem-vindo");
            boolean exit = false;
            while(!exit){
                System.out.println("""
                        Escolha uma opção:
                        1- Enviar tarefa
                        2- Verificar estado
                        0- Sair""");
                String input = sysIn.readLine();
                if(!isDigit(input))
                {
                    System.out.println("Insira um valor válido.");
                    continue;
                }
                int choice = Integer.parseInt(input);
                switch (choice)
                {
                    case 1:
                        String mem = readMemory();
                        File file = readFile();
                        String task = getTaskFromFile(file);

                        new Thread(() -> {
                            try {
                                sendTask(mem, task);
                                receiveTask(file.getPath());
                            }
                            catch (Exception e)
                            {
                                System.out.println("Erro a enviar task: " + e);
                            }
                        }).start();
                        break;

                    case 2:
                        new Thread(() -> {
                            try {
                                status();
                            } catch (Exception e)
                            {
                                System.out.println("Erro a pedir status: " + e);
                            }
                        }).start();
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
        }
        catch (IOException e)
        {
            System.out.println("Erro de IO: " + e);
        }
    }

    private static void fixResultPath(String path)
    {
        RESULT_PATH = path;
        if(RESULT_PATH.charAt(RESULT_PATH.length() - 1) != '/')
            RESULT_PATH = RESULT_PATH.concat("/");
    }

    private static void login() throws IOException
    {
        while(true)
        {
            System.out.println("Insira nome: ");
            String name = sysIn.readLine();
            System.out.println("Insira password: ");
            String pass = sysIn.readLine();

            //Envia nome e pass como nome;pass
            out.writeUTF(MessageTypes.LOGIN.typeToString());
            out.writeUTF(name.concat(";").concat(pass));
            out.flush();
            if(in.readUTF().equals("no"))
            {
                System.out.println("Credenciais erradas, tente denovo.");
                continue;
            }
            break;
        }
    }

    private static boolean isDigit(String input)
    {
        if(input == null || input.isBlank() || input.isEmpty())
            return false;
        for (char c : input.toCharArray()) {
            if(!Character.isDigit(c))
                return false;
        }
        return true;
    }

    private static void disconnect(Socket socket) throws IOException
    {
        socket.shutdownOutput();
        socket.shutdownInput();
        socket.close();
        System.out.println("Disconectado.");
    }

    private static void sendTask(String mem, String task) throws IOException
    {
        out.writeUTF(MessageTypes.TASK_REQUEST.typeToString());
        out.writeInt(Integer.parseInt(mem));
        out.write(task.getBytes());
        out.flush();
    }

    private static void receiveTask(String path)
    {
        path = path.concat("_Result");
        try{
            File file = new File(RESULT_PATH.concat(path));
            if(file.createNewFile())
            {
                try(FileOutputStream writer = new FileOutputStream(file, false))
                {
                    writer.write(in.readAllBytes());
                }
            }
            else
            {
                throw new Exception("Não foi possível criar ficheiro.");
            }
        }
        catch (Exception e)
        {
            System.out.println("Erro a guardar resultados: " + e);
        }
    }

    private static void status() throws IOException
    {
        out.writeUTF(MessageTypes.STATUS.typeToString());
        int mem = in.readInt();
        int nQueue = in.readInt();
        System.out.println("Memória disponível: " + mem + "\nTarefas pendentes: " + nQueue);
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
}