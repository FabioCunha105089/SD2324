package src;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
public class Menu {
    private final BufferedReader sysIn;

    public Menu() {
        this.sysIn = new BufferedReader(new InputStreamReader(System.in));
    }

    public void displayInitialMenu() {
        System.out.println("""
                Escolha uma opção:
                1- Registar
                2- Login
                0- Sair""");
    }

    public void displayMainMenu() {
        System.out.println("""
                Escolha uma opção:
                1- Realizar tarefa
                2- Pedir status
                0- Sair""");
    }

    public String getFilePath() throws IOException {
        System.out.println("Insira o caminho do ficheiro: ");
        return sysIn.readLine();
    }

    public String[] getUserCredentials() throws IOException {
        System.out.println("Insira nome: ");
        String name = sysIn.readLine();
        System.out.println("Insira password: ");
        String pass = sysIn.readLine();
        return new String[]{name, pass};
    }

    public int getUserChoice() {
        int choice = -1;
        while (choice < 0 || choice > 2) {
            String input = null;
            try {
                input = sysIn.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (isDigit(input))
                choice = Integer.parseInt(input);
        }
        return choice;
    }

    private boolean isDigit(String input) {
        if (input == null || input.isBlank() || input.isEmpty())
            return false;
        for(char c : input.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }

    public void printStatus(Status status) {
        System.out.println("Tarefas na fila de espera: " + status.getQueueSize());
        if (status.getWorkersInfo() != null) {
            for (String info : status.getWorkersInfo())
                System.out.println(info);
        }
        System.out.println("\nPressionar Enter para voltar ao menu principal...");
        try {
            sysIn.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
