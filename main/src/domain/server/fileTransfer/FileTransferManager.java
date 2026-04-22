package domain.server.fileTransfer;


import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class FileTransferManager {
    private final ConcurrentHashMap<String, FileTransfer> transfers = new ConcurrentHashMap<>();

    public void addFileTransfer(String fileTransferId, String sender, String receiver) {
        transfers.put(fileTransferId, new FileTransfer(sender, receiver));
    }

    public void removeFileTransfer(String fileTransferId) {
        transfers.remove(fileTransferId);
    }

    public FileTransfer getFileTransfer(String fileTransferId) {
        return transfers.get(fileTransferId);
    }

    public boolean containsKey(String key) {
        return transfers.containsKey(key);
    }

    public void setSocket(String fileTransferId, String username, Socket socket) {
        getFileTransfer(fileTransferId).setSocket(username, socket);
    }

    public void setOnFileTransferCompletion(String fileTransferId, Runnable onCompletion) {
        getFileTransfer(fileTransferId).setOnCompletion(onCompletion);
    }

    public void waitForResponse(String fileTransferId, Runnable onPositiveResponse, Runnable onNegativeResponse) {
        try {
            getFileTransfer(fileTransferId).waitForAnswer(onPositiveResponse, onNegativeResponse);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void accept(String fileTransferId) {
        getFileTransfer(fileTransferId).accept();
    }

    public void reject(String fileTransferId) {
        getFileTransfer(fileTransferId).reject();
    }
}
