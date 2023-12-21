package src;

public class ClientGUI {
    private static final Menus menuHandler = new Menus();
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("É necessário o caminho para a pasta onde serão guardados os ficheiros.");
            System.exit(0);
        }

        ClientLib.fixResultPath(args[0]);
        ClientLib.connectToServer();

        boolean loggedIn = false;
        while(!loggedIn) {
            menuHandler.clearTerminal();
            menuHandler.displayInitialMenu();
            int choice = menuHandler.getUserChoice();
            if(choice != 0) {
                menuHandler.clearTerminal();
                String[] credentials = menuHandler.getUserCredentials();
                loggedIn = ClientLib.validateUser(choice, credentials);
            }else
                break;
            menuHandler.waitForEnter();
        }
        if (loggedIn) {
            boolean exit = false;
            while(!exit) {
                menuHandler.clearTerminal();
                menuHandler.displayMainMenu();

                int choice = menuHandler.getUserChoice();
                String filePath;

                menuHandler.clearTerminal();

                if(choice == 1) {
                    filePath = menuHandler.getFilePath();
                }else
                    filePath = null;
                exit = ClientLib.startProgram(choice, filePath);

                if (choice == 2)
                    menuHandler.waitForEnter();
            }
        }
        ClientLib.disconnect();
    }
}
