package domain.client;

import domain.utils.constants.Codes;
import domain.utils.constants.StatusCodes;
import domain.utils.ConvertMessageUtil;
import domain.utils.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.function.Consumer;

public class Listener {
    private final BufferedReader reader;
    private final Consumer<Message> handleMessage;

    public Listener(
            InputStream inputStream,
            Consumer<Message> handleMessage
    ) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        this.handleMessage = handleMessage;
    }

    public void listenToServer() {
        new Thread(() -> {
            try {
                readInput();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        System.out.println("Listening to domain.server.");
    }

    private void readInput() throws IOException {
        while (true) {
            String line = reader.readLine();

            if (line == null) {
                return;
            }

            switch (ConvertMessageUtil.messageToObject(line)) {
                case Hi hi -> {
                    System.out.println("Client connected successfully! Server version: " + hi.version());
                    handleMessage.accept(hi);
                }
                case Ping ping -> handleMessage.accept(ping);
                case Hangup hangup -> System.out.println(Codes.get(hangup.reason()));
                case PongError pongError -> System.out.println(Codes.get(pongError.code()));
                case ParseError _ -> System.out.println("Parse error.");

                case Joined joined -> System.out.printf("%s joined the chat.%n", joined.username());

                case Left left -> System.out.printf("%s left the chat.%n", left.username());
                case Broadcast broadcast -> System.out.printf("%s: %s%n", broadcast.username(), broadcast.message());
                case Private privateMessage -> System.out.printf("%s: %s%n", privateMessage.senderUsername(), privateMessage.message());

                case CoinTossStart coinTossStart -> {
                    System.out.println("Started a coin toss game with " + coinTossStart.username());
                    handleMessage.accept(coinTossStart);
                }
                case CoinTossResult coinTossResult -> handleCoinTossResult(coinTossResult);
                case CoinTossChoice _ -> System.out.println("Choose heads or tails");
                case CoinTossWin coinTossWin -> {
                    System.out.println("You won!");
                    handleMessage.accept(coinTossWin);
                }
                case CoinTossLose coinTossLose -> {
                    System.out.println("You lost...");
                    handleMessage.accept(coinTossLose);
                }

                case DownloadAsk downloadAsk -> {
                    System.out.printf("%s wants to send you a file (%s). Do you accept? (yes/no)%n", downloadAsk.sender(), downloadAsk.filename());
                    handleMessage.accept(downloadAsk);
                }
                case TransferAccept transferAccept -> {
                    System.out.println("Transfer request accepted. Proceeding with upload.");
                    handleMessage.accept(transferAccept);
                }
                case TransferReject transferReject -> {
                    System.out.println("Transfer request rejected.");
                    handleMessage.accept(transferReject);
                }
                case TransferDone transferDone -> {
                    System.out.println("Transfer done.");
                    handleMessage.accept(transferDone);
                }
                case DownloadDone downloadDone -> {
                    System.out.println("Download done.");
                    handleMessage.accept(downloadDone);
                }

                case LogonResp logonResp -> handleMessage.accept(logonResp);
                case BroadcastResp broadcastResp -> handleGeneralResp(broadcastResp.status(), broadcastResp.code(), "Sent message successfully.");
                case PrivateResp privateResp -> handleGeneralResp(privateResp.status(), privateResp.code(), "Sent message successfully.");
                case ListResp listResp -> handleListUsersResp(listResp);
                case CoinTossResp coinTossResp -> handleGeneralResp(coinTossResp.status(), coinTossResp.code(), "Initialized game successfully.");
                case TransferResp transferResp -> handleGeneralResp(transferResp.status(), transferResp.code(), "Sent file transfer request successfully.");
                case DownloadAcceptResp downloadAcceptResp -> handleGeneralResp(downloadAcceptResp.status(), downloadAcceptResp.code(), "Accepted file transfer successfully. Proceeding with download.");
                case DownloadRejectResp downloadRejectResp -> {
                    handleGeneralResp(downloadRejectResp.status(), downloadRejectResp.code(), "Rejected download successfully.");
                    handleMessage.accept(downloadRejectResp);
                }
                case ByeResp byeResp -> handleGeneralResp(byeResp.status(), byeResp.code(), "Logged out successfully.");

                default -> throw new IllegalStateException("Unexpected value: " + line);
            }
        }
    }

    private void handleGeneralResp(String statusCode, int code, String successMessage) {
        System.out.println(statusCode.equals(StatusCodes.OK) ? successMessage : Codes.get(code));
    }

    private void handleListUsersResp(ListResp listResp) {
        System.out.println("All users:");
        for (String username : listResp.usernames()) {
            System.out.println(username);
        }
    }

    private void handleCoinTossResult(CoinTossResult coinTossResult) {
        System.out.println("Round scores:");
        for (Map.Entry<String, Integer> entry : coinTossResult.scores().entrySet()) {
            System.out.printf("%s: %s%n", entry.getKey(), entry.getValue());
        }
    }
}
