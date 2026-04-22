package domain.server.fileTransfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.server.sessions.ClientSession;
import domain.utils.ConvertMessageUtil;
import domain.utils.constants.Codes;
import domain.utils.constants.StatusCodes;
import domain.utils.messages.Message;
import domain.utils.messages.TransferFile;
import domain.utils.messages.TransferResp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import static domain.server.Main.transfersManager;

public class FileTransferSocketManager implements Runnable {
    private final Socket socket;
    private final PrintWriter printWriter;
    private final Map<String, ClientSession> sessions;

    public FileTransferSocketManager(Socket socket, Map<String, ClientSession> sessions) {
        this.socket = socket;
        this.sessions = sessions;
        try {
            this.printWriter = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line = reader.readLine();
            Message message = ConvertMessageUtil.messageToObject(line);

            if (message instanceof TransferFile transferFile) {
                handleTransferFile(transferFile);
            } else {
                throw new IllegalStateException("Expected TRANSFER_FILE, got: " + message);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleTransferFile(TransferFile transferFile) {
        if (!transfersManager.containsKey(transferFile.transferId())) {
            send(new TransferResp(StatusCodes.ERROR, Codes.INVALID_TRANSFER_ID));
        } else if (!sessions.containsKey(transferFile.username())) {
            send(new TransferResp(StatusCodes.ERROR, Codes.NON_EXISTENT_USERNAME_PROVIDED));
        } else {
            transfersManager.setSocket(transferFile.transferId(), transferFile.username(), socket);
        }
    }

    private void send(Message message) {
        try {
            printWriter.println(ConvertMessageUtil.objectToMessage(message));
            printWriter.flush();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
