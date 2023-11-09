package grupo30;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client
{
    //Receber no socket
    private static BufferedReader in;
    //Enviar no socket
    private static PrintWriter out;
    //Ler do teclado
    private static BufferedReader sysIn;
    public static void main(String[] args)
    {
        try (Socket socket = new Socket("localhost", 9090)){
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            sysIn = new BufferedReader(new InputStreamReader(System.in));
            login();
            System.out.println("Bem-vindo");
            boolean exit = false;
            while(!exit){
                System.out.println("""
                        Escolha uma opção:
                        1- Enviar tarefa
                        2- Sair""");
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
                        sendTask();
                        break;
                    case 2:
                        disconnect(socket);
                        exit = true;
                        break;
                    default:
                        break;
                }
            }
        }
        catch (Exception e) {
            System.out.println("Exception na conexão: " + e);
        }
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
            out.println(name.concat(";").concat(pass));
            out.flush();
            if(in.readLine().equals("no"))
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
    }

    private static void sendTask() throws IOException
    {
        String mem;
        while(true) {
            System.out.println("Quanta memória vai precisar?:");
            mem = sysIn.readLine();
            if (isDigit(mem))
                break;
            System.out.println("Insira um valor válido.");
        }
        System.out.println("Insira o caminho para o ficheiro com a tarefa:");
        String path = sysIn.readLine();
        String[] tasks = null;
        try{
            File file = new File(path);
            Scanner scanner = new Scanner(file);
            //Estrutura do ficheiro: Nº de tasks na primeira linha, tasks nas linhas seguintes REVER
            int nTasks = scanner.nextInt();
            tasks = new String[nTasks];
            for (int i = 0; i < nTasks; i++) {
                tasks[i] = scanner.nextLine();
            }
        }
        catch (Exception e)
        {
            System.out.println("Exception a ler ficheiro: " + e);
        }
        //Primeiro envia a quantidade de memória que vai precisar, depois as tasks REVER
        out.println(mem);
        for (String task: tasks) {
            out.println(task);
        }
        out.flush();
    }
}