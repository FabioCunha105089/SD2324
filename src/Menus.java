package src;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
public class Menus {
    private final BufferedReader sysIn;

    public Menus() {
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

    public String getFilePath() {
        System.out.println("Insira o caminho do ficheiro: ");
        try {
            return sysIn.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getUserCredentials(){
        try {
            System.out.println("Insira nome: ");
            String name = sysIn.readLine();
            System.out.println("Insira password: ");
            String pass = sysIn.readLine();
            return new String[]{name, pass};
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getUserChoice() {
        int choice = -1;
        while (choice < 0 || choice > 2) {
            String input;
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

    public void waitForEnter() {
        try {
            System.out.print("\nPressionar Enter para continuar...");
            sysIn.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearTerminal() {
        /*for (int i = 0; i < 50; ++i) {
            System.out.println();
        }*/
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
