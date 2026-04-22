package domain.client;

import java.io.*;
import java.net.Socket;

import domain.utils.MessageSender;
import domain.utils.constants.Codes;
import domain.utils.constants.StatusCodes;
import domain.utils.messages.*;

import static domain.client.constants.UserCommands.*;
import static domain.utils.constants.ConnectionConfig.*;

public class Client {
    private final MessageSender messageSender;
    private final Listener listener;
    private final CLIManager cliManager;
    private final FileTransferManager transferManager;

    public Client() throws IOException {
        Socket socket = new Socket(HOST, SERVER_PORT);
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        messageSender = new MessageSender(new PrintWriter(outputStream));
        listener = new Listener(
                inputStream,
                this::handleServerMessage
        );
        transferManager = new FileTransferManager();
        cliManager = new CLIManager(messageSender, transferManager);
    }

    public void start() {
        listener.listenToServer();
    }

    private void handleServerMessage(Message message) {
        switch (message) {
            case Hi _ -> logon();
            case Ping _ -> onPing();
            case LogonResp logonResp -> onLogon(logonResp);
            case CoinTossStart coinTossStart -> cliManager.setCurrentGameId(coinTossStart.gameId());
            case CoinTossWin _, CoinTossLose _ -> cliManager.setCurrentGameId(null);
            case DownloadAsk downloadAsk -> {
                transferManager.setTransferId(downloadAsk.transferId());
                transferManager.setFilename(downloadAsk.filename());
                transferManager.setReceivedChecksum(downloadAsk.checksum());
            }
            case TransferAccept transferAccept -> {
                transferManager.setTransferId(transferAccept.transferId());
                transferManager.upload();
            }
            case TransferReject _, TransferDone _, DownloadDone _, DownloadRejectResp _ -> transferManager.setTransferId(null);
            default -> System.out.println("Unhandled message: " + message);
        }
    }

    private void logon() {
        String username = cliManager.promptLogin();
        transferManager.setUsername(username);
        messageSender.sendMessage(new Logon(username));
    }

    private void onPing() {
        messageSender.sendMessage(new Pong());
    }

    private void onLogon(LogonResp logonResp) {
        if (logonResp.status().equals(StatusCodes.OK)) {
            System.out.printf(
                    "Logged in successfully. Use '%s' to get a list of commands%n",
                    HELP
            );
            new Thread(cliManager).start();
        } else {
            System.out.println(Codes.get(logonResp.code()));
            logon();
        }
    }
}
