package domain.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.utils.MessageSender;
import domain.server.game.CoinTossGame;
import domain.server.sessions.WritableSession;
import domain.utils.ConvertMessageUtil;
import domain.utils.constants.Codes;
import domain.utils.constants.StatusCodes;
import domain.utils.messages.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static domain.server.Main.gamesManager;
import static domain.server.Main.transfersManager;
import static domain.utils.constants.HeadsAndTails.HEADS;
import static domain.utils.constants.HeadsAndTails.TAILS;

public class ServerMessageManager extends MessageSender {
    private final WritableSession session;
    private final Supplier<Map<String, WritableSession>> writableSessionsSupplier;
    private final Runnable onLogon;

    public ServerMessageManager(WritableSession writableSession, Supplier<Map<String, WritableSession>> writableSessionsSupplier, Runnable onLogon) {
        super(writableSession.getPrintWriter());
        this.session = writableSession;
        this.printWriter = writableSession.getPrintWriter();
        this.writableSessionsSupplier = writableSessionsSupplier;
        this.onLogon = onLogon;
    }

    public void sendToOtherUser(String username, Message message) {
        PrintWriter receiverPrintWriter = writableSessionsSupplier.get().get(username).getPrintWriter();
//        System.out.printf("Sending other %s%n", message);
        try {
            receiverPrintWriter.println(ConvertMessageUtil.objectToMessage(message));
            receiverPrintWriter.flush();
        } catch (JsonProcessingException jsonProcessingException) {
            System.err.println(jsonProcessingException.getMessage());
        }
    }

    public void sendUsersList() {
        List<String> usernames = writableSessionsSupplier.get().values().stream()
                .map(WritableSession::getUsername)
                .toList();

        try {
            printWriter.println(ConvertMessageUtil.objectToMessage(new ListResp(StatusCodes.OK, usernames)));
            printWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleBroadcastReq(BroadcastReq broadcastReq) {
        if (!isLoggedIn()) {
            sendMessage(new BroadcastResp(StatusCodes.ERROR, Codes.NOT_LOGGED_IN));
        } else {
            sendMessage(new BroadcastResp(StatusCodes.OK, Codes.OK));
            broadcastMessage(new Broadcast(session.getUsername(), broadcastReq.message()));
        }
    }

    public void broadcastMessage(Message message) {
        System.out.printf("Broadcasting %s%n", message);
        List<PrintWriter> printWriters = writableSessionsSupplier.get().values().stream()
                .filter((s) -> !s.getUsername().equals(session.getUsername()))
                .map(WritableSession::getPrintWriter)
                .toList();

        try {
            for (PrintWriter otherPrintWriter : printWriters) {
                otherPrintWriter.println(ConvertMessageUtil.objectToMessage(message));
                otherPrintWriter.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handlePrivateReq(PrivateReq privateReq) {
        if (!isLoggedIn()) {
            sendMessage(new PrivateResp(StatusCodes.ERROR, Codes.NOT_LOGGED_IN));
        } else if (!writableSessionsSupplier.get().containsKey(privateReq.username())) {
            sendMessage(new PrivateResp(StatusCodes.ERROR, Codes.NON_EXISTENT_USERNAME_PROVIDED));
        } else {
            sendPrivateMessage(privateReq.username(), privateReq.message());
        }
    }

    private void sendPrivateMessage(String username, String message) {
        sendMessage(new PrivateResp(StatusCodes.OK, Codes.OK));
        sendToOtherUser(username, new Private(session.getUsername(), message));
    }

    public void handleCoinTossReq(CoinTossReq coinTossReq) {
        if (!isLoggedIn()) {
            sendMessage(new CoinTossResp(StatusCodes.ERROR, Codes.NOT_LOGGED_IN));
        } else if (!writableSessionsSupplier.get().containsKey(coinTossReq.username())) {
            sendMessage(new CoinTossResp(StatusCodes.ERROR, Codes.NON_EXISTENT_USERNAME_PROVIDED));
        } else {
            startCoinTossGame(coinTossReq.username());
        }
    }

    private void startCoinTossGame(String otherPlayerUsername) {
        sendMessage(new CoinTossResp(StatusCodes.OK, Codes.OK));

        String gameId = UUID.randomUUID().toString();
        sendMessage(new CoinTossStart(otherPlayerUsername, gameId));
        sendToOtherUser(otherPlayerUsername, new CoinTossStart(session.getUsername(), gameId));
        gamesManager.addGame(gameId, session.getUsername(), otherPlayerUsername, 3);

        sendMessage(new CoinTossChoice());
        sendToOtherUser(otherPlayerUsername, new CoinTossChoice());
    }

    public void handleHeads(Heads heads) {
        CompletableFuture.runAsync(() -> makeChoice(heads.gameId(), HEADS));
    }

    public void handleTails(Tails tails) {
        CompletableFuture.runAsync(() -> makeChoice(tails.gameId(), TAILS));
    }

    private void makeChoice(String gameId, boolean choice) {
        try {
            CoinTossGame game = gamesManager.getGame(gameId);

            if (game == null) {
                sendMessage(new CoinTossResp(StatusCodes.ERROR, Codes.INVALID_GAME_ID));
                return;
            }

            String username = session.getUsername();
            Map<String, Integer> results = game.putChoice(username, choice);
            sendMessage(new CoinTossResult(results));
            if (game.hasWon(username)) {
                sendMessage(new CoinTossWin());

                String player1 = game.getPlayer1();
                if (player1.equals(username)) {
                    sendToOtherUser(game.getPlayer2(), new CoinTossLose());
                } else {
                    sendToOtherUser(player1, new CoinTossLose());
                }
                gamesManager.removeGame(gameId);
            }

            if (!game.isFinished()) {
                sendMessage(new CoinTossChoice());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleTransferReq(TransferReq transferReq) {
        String receiver = transferReq.receiver();

        if (!isLoggedIn()) {
            sendMessage(new TransferResp(StatusCodes.ERROR, Codes.NOT_LOGGED_IN));
        } else if (!writableSessionsSupplier.get().containsKey(transferReq.receiver())) {
            sendMessage(new TransferResp(StatusCodes.ERROR, Codes.NON_EXISTENT_USERNAME_PROVIDED));
        } else {
            String transferId = UUID.randomUUID().toString();
            transfersManager.addFileTransfer(transferId, session.getUsername(), receiver);

            CompletableFuture.runAsync(() -> {
                transfersManager.waitForResponse(
                        transferId,
                        () -> {
                            sendMessage(new TransferAccept(transferId));
                            transfersManager.setOnFileTransferCompletion(transferId, () -> {
                                sendMessage(new TransferDone());
                                sendToOtherUser(receiver, new DownloadDone());
                                transfersManager.removeFileTransfer(transferId);
                            });
                        },
                        () -> {
                            sendMessage(new TransferReject(transferId));
                            transfersManager.removeFileTransfer(transferId);
                        }
                );
            });
            sendMessage(new TransferResp(StatusCodes.OK, Codes.OK));
            sendToOtherUser(receiver, new DownloadAsk(session.getUsername(), transferReq.filename(), transferReq.checksum(), transferId));
        }
    }

    public void handleDownloadAccept(DownloadAccept downloadAccept) {
        if (transfersManager.getFileTransfer(downloadAccept.transferId()) == null) {
            sendMessage(new DownloadAcceptResp(StatusCodes.ERROR, Codes.INVALID_TRANSFER_ID));
        } else {
            sendMessage(new DownloadAcceptResp(StatusCodes.OK, Codes.OK));
            transfersManager.accept(downloadAccept.transferId());
        }
    }

    public void handleDownloadReject(DownloadReject downloadReject) {
        if (transfersManager.getFileTransfer(downloadReject.transferId()) == null) {
            sendMessage(new DownloadRejectResp(StatusCodes.ERROR, Codes.INVALID_TRANSFER_ID));
        } else {
            sendMessage(new DownloadRejectResp(StatusCodes.OK, Codes.OK));
            transfersManager.reject(downloadReject.transferId());
        }
    }

    public void logon(String username) {
        int code = validateUsername(username);

        if (code == Codes.OK) {
            session.setUsername(username);
            onLogon.run();
            sendMessage(new LogonResp(StatusCodes.OK, Codes.OK));
            broadcastMessage(new Joined(session.getUsername()));
        } else {
            sendMessage(new LogonResp(StatusCodes.ERROR, code));
        }
    }

    private int validateUsername(String username) {
        if (writableSessionsSupplier.get().containsKey(username) && !isLoggedIn()) {
            return Codes.USERNAME_ALREADY_EXISTS;
        } else if (username == null || !username.matches("^[A-Za-z0-9_]{3,14}$")) {
            return Codes.USERNAME_HAS_INVALID_FORMAT_OR_LENGTH;
        } else if (isLoggedIn()) {
            return Codes.ALREADY_LOGGED_IN;
        } else {
            return Codes.OK;
        }
    }

    private boolean isLoggedIn() {
        return !session.getUsername().isEmpty();
    }
}
