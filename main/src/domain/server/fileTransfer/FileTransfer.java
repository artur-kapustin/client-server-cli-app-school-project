package domain.server.fileTransfer;

import java.io.IOException;
import java.net.Socket;

public class FileTransfer {
    private final String sender;
    private final String receiver;
    private Socket senderSocket;
    private Socket receiverSocket;
    private boolean accepted;
    private Runnable onCompletion;

    public FileTransfer(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }

    public synchronized void setSocket(String username, Socket socket) {
        if (username.equals(sender)) {
            this.senderSocket = socket;
        } else if (username.equals(receiver)) {
            this.receiverSocket = socket;
        } else {
            throw new IllegalArgumentException("Username must belong to either sender or receiver");
        }

        if (senderSocket != null && receiverSocket != null) {
            initiateFileTransfer();
        }
    }

    public void setOnCompletion(Runnable onCompletion) {
        this.onCompletion = onCompletion;
    }

    public synchronized void waitForAnswer(Runnable onPositiveResponse, Runnable onNegativateResponse) throws InterruptedException {
        wait();

        if (accepted) {
            onPositiveResponse.run();
        } else {
            onNegativateResponse.run();
        }
    }

    public synchronized void accept() {
        accepted = true;
        notifyAll();
    }

    public synchronized void reject() {
        accepted = false;
        notifyAll();
    }

    public void initiateFileTransfer() {
        new Thread(() -> {
            try {
                senderSocket.getInputStream().transferTo(receiverSocket.getOutputStream());

                receiverSocket.shutdownOutput();
                senderSocket.shutdownInput();

                receiverSocket.close();
                senderSocket.close();

                if (onCompletion != null) {
                    onCompletion.run();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
