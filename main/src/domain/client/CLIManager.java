package domain.client;

import domain.utils.MessageSender;
import domain.utils.messages.*;

import java.io.IOException;
import java.util.Scanner;

import static domain.client.constants.UserCommands.*;

public class CLIManager implements Runnable {
    private final Scanner scanner;
    private final MessageSender messageSender;
    private final FileTransferManager transferManager;
    private boolean isRunning;
    private String currentGameId;

    public CLIManager(MessageSender messageSender, FileTransferManager transferManager) {
        this.scanner = new Scanner(System.in);
        this.messageSender = messageSender;
        this.transferManager = transferManager;
        this.isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            chooseCommand(scanner.nextLine());
        }
    }

    public void setCurrentGameId(String currentGameId) {
        this.currentGameId = currentGameId;
    }

    public String promptLogin() {
        System.out.print("Enter your username: ");
        return scanner.nextLine();
    }

    private void chooseCommand(String command) {
        switch (command) {
            case HELP -> handleHelp();
            case MESSAGE -> handleMessage();
            case PRIVATE -> handlePrivate();
            case LIST_USERS -> handleListUsers();
            case START_COIN_GAME -> handleCoinTossStart();
            case HEADS_CHOICE -> handleHeads();
            case TAILS_CHOICE -> handleTails();
            case TRANSFER_FILE -> handleFileTransferRequest();
            case YES -> handleDownloadAccept();
            case NO -> handleDownloadReject();
            case EXIT -> handleExit();
            default -> System.out.printf("Unknown command please use '%s' to see the list of commands.", HELP);
        }
    }

    private void handleMessage() {
        System.out.print("Write your message: ");
        messageSender.sendMessage(new BroadcastReq(scanner.nextLine()));
    }

    private void handlePrivate() {
        System.out.print("Enter username of a user to whom you want to send the message: ");
        String username = scanner.nextLine();
        System.out.print("Enter your message: ");
        String message = scanner.nextLine();
        messageSender.sendMessage(new PrivateReq(message, username));
    }

    private void handleListUsers() {
        messageSender.sendMessage(new ListReq());
    }

    private void handleCoinTossStart() {
        if (currentGameId != null) {
            System.out.println("Please finish the game you are currently playing before starting a new one.");
            return;
        }

        System.out.print("Enter the other player's username: ");
        messageSender.sendMessage(new CoinTossReq(scanner.nextLine()));
    }

    private void handleHeads() {
        messageSender.sendMessage(new Heads(currentGameId));
    }

    private void handleTails() {
        messageSender.sendMessage(new Tails(currentGameId));
    }

    private void handleFileTransferRequest() {
        if (transferManager.isTransferInProgress()) {
            System.out.println("Please finish the current file transfer before starting a new one.");
            return;
        }

        System.out.print("Enter the receiver's username: ");
        String receiverUsername = scanner.nextLine();
        System.out.print("Enter filepath: ");
        String fileInPath = scanner.nextLine();

        transferManager.setFileInPath(fileInPath);
        int lastSlashIndex = fileInPath.lastIndexOf("/");
        try {
            messageSender.sendMessage(new TransferReq(
                    receiverUsername,
                    fileInPath.substring(lastSlashIndex + 1),
                    transferManager.getSha256Hex()
            ));
        } catch (IOException e) {
            System.out.println("Incorrect file path. Please try again.");
        }
    }

    private void handleDownloadAccept() {
        if (transferManager.isTransferInProgress()) {
            messageSender.sendMessage(new DownloadAccept(transferManager.getTransferId()));
            transferManager.download();
        } else {
            System.out.println("There is no pending file transfer request.");
        }
    }

    private void handleDownloadReject() {
        if (transferManager.isTransferInProgress()) {
            messageSender.sendMessage(new DownloadReject(transferManager.getTransferId()));
        } else {
            System.out.println("There is no pending file transfer request.");
        }
    }

    private void handleExit() {
        messageSender.sendMessage(new Bye());
        isRunning = false;
    }

    private void handleHelp() {
        System.out.printf(
                """
                        %s - see this list again
                        %s - broadcast a message
                        %s - send a private message
                        %s - list users
                        %s - start coin game with a specified user
                        %s - choose heads during the game
                        %s - choose tails during the game
                        %s - request to transfer tile to a specified user
                        %s - accept a file transfer
                        %s - decline a file transfer
                        %s - exit the application
                        
                        """,
                HELP,
                MESSAGE,
                PRIVATE,
                LIST_USERS,
                START_COIN_GAME,
                HEADS_CHOICE,
                TAILS_CHOICE,
                TRANSFER_FILE,
                YES,
                NO,
                EXIT
        );
    }
}
