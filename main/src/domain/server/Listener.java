package domain.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.server.sessions.ReadableSession;
import domain.utils.ConvertMessageUtil;
import domain.utils.messages.*;

import java.io.BufferedReader;
import java.io.IOException;

public class Listener {
    private final BufferedReader bufferedReader;
    private final ServerMessageManager serverMessageManager;
    private final Runnable closeConnection;
    private final Runnable onPong;

    public Listener(ReadableSession readableSession, ServerMessageManager serverMessageManager, Runnable closeConnection, Runnable onPong) {
        this.bufferedReader = readableSession.getBufferedReader();
        this.serverMessageManager = serverMessageManager;
        this.closeConnection = closeConnection;
        this.onPong = onPong;
    }

    public void listenToClient() {
        new Thread(() -> {
            try {
                readInput();
            } catch (IOException e) {
                System.err.println("Connection closed.");
            }
        }).start();
        System.out.println("Listening to domain.server.");
    }

    private void readInput() throws IOException {
        while (true) {
            try {
                String line = bufferedReader.readLine();

                if (line == null) {
                    closeConnection.run();
                    return;
                }

                System.out.println(line);
                switch (ConvertMessageUtil.messageToObject(line)) {
                    case Logon logon -> serverMessageManager.logon(logon.username());
                    case Pong _ -> onPong.run();
                    case BroadcastReq broadcastReq -> serverMessageManager.handleBroadcastReq(broadcastReq);
                    case PrivateReq privateReq -> serverMessageManager.handlePrivateReq(privateReq);
                    case ListReq _ -> serverMessageManager.sendUsersList();
                    case CoinTossReq coinTossReq -> serverMessageManager.handleCoinTossReq(coinTossReq);
                    case Heads heads -> serverMessageManager.handleHeads(heads);
                    case Tails tails -> serverMessageManager.handleTails(tails);
                    case TransferReq transferReq -> serverMessageManager.handleTransferReq(transferReq);
                    case DownloadAccept downloadAccept -> serverMessageManager.handleDownloadAccept(downloadAccept);
                    case DownloadReject downloadReject -> serverMessageManager.handleDownloadReject(downloadReject);
                    case Bye _ -> closeConnection.run();
                    default -> serverMessageManager.sendMessage(new UnknownCommand());
                }
            } catch (JsonProcessingException e) {
                serverMessageManager.sendMessage(new ParseError());
            }
        }
    }
}
