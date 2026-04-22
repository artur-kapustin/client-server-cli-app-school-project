package domain.client;

import domain.utils.MessageSender;
import domain.utils.HashUtil;
import domain.utils.messages.TransferFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;

import static domain.utils.constants.ConnectionConfig.FILE_TRANSFER_PORT;
import static domain.utils.constants.ConnectionConfig.HOST;

public class FileTransferManager {
    private final static String FILE_OUT_PATH = "main/file-out/";

    private String transferId;
    private String fileInPath;
    private String filename;
    private String receivedChecksum;
    private String username;

    public synchronized String getTransferId() {
        return transferId;
    }

    public synchronized void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public void setFileInPath(String fileInPath) {
        this.fileInPath = fileInPath;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setReceivedChecksum(String receivedChecksum) {
        this.receivedChecksum = receivedChecksum;
    }

    public boolean isTransferInProgress() {
        return transferId != null;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void download() {
        new Thread(() -> {
            try (OutputStream fileOut = Files.newOutputStream(Path.of(FILE_OUT_PATH + filename));
                 Socket socket = new Socket(HOST, FILE_TRANSFER_PORT);
                 InputStream socketIn = socket.getInputStream()
            ) {
                DigestOutputStream digestOut = HashUtil.sha256OutputStream(fileOut);

                new MessageSender(new PrintWriter(socket.getOutputStream())).sendMessage(new TransferFile(username, transferId));

                socketIn.transferTo(digestOut);
                digestOut.flush();
                socket.shutdownInput();

                String computedChecksum = HashUtil.messageDigestToHex(digestOut.getMessageDigest());
                if (!computedChecksum.equals(receivedChecksum)) {
                    System.err.println("CHECKSUM DOES NOT MATCH. Deleting corrupted file.");
                    Files.deleteIfExists(Path.of(FILE_OUT_PATH + filename));
                } else {
                    System.out.println("Checksum matched. File transfer successful. Checksum: " + receivedChecksum);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void upload() {
        new Thread(() -> {
            try (InputStream fileIn = Files.newInputStream(Path.of(fileInPath));
                 Socket socket = new Socket(HOST, FILE_TRANSFER_PORT)
            ) {
                OutputStream socketOut = socket.getOutputStream();

                new MessageSender(new PrintWriter(socketOut)).sendMessage(new TransferFile(username, transferId));

                fileIn.transferTo(socketOut);
                socketOut.flush();
                socket.shutdownOutput();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public String getSha256Hex() throws IOException {
        if (fileInPath == null) {
            throw new IllegalStateException("filePath is not set");
        }

        try (InputStream fileIn = Files.newInputStream(Path.of(fileInPath))) {
            return HashUtil.sha256(fileIn);
        }
    }
}
