package domain.server;

import domain.server.fileTransfer.FileTransferManager;
import domain.server.fileTransfer.FileTransferSocketManager;
import domain.server.game.CoinTossGamesManager;
import domain.server.sessions.ClientSession;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static domain.utils.constants.ConnectionConfig.FILE_TRANSFER_PORT;
import static domain.utils.constants.ConnectionConfig.SERVER_PORT;

public class Main {
    private static final Map<String, ClientSession> sessions = Collections.synchronizedMap(new HashMap<>());
    public static final CoinTossGamesManager gamesManager = new CoinTossGamesManager();
    public static final FileTransferManager transfersManager = new FileTransferManager();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        ServerSocket fileTransferSocket = new ServerSocket(FILE_TRANSFER_PORT);
        new Thread(() -> {
            while (true) {
                try {
                    new Thread(new ClientSessionManager(serverSocket.accept(), sessions)).start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        ).start();

        new Thread(() -> {
            while (true) {
                try {
                    new Thread(new FileTransferSocketManager(fileTransferSocket.accept(), sessions)).start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        ).start();
    }
}
