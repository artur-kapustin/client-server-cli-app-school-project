package domain.server.sessions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSession implements WritableSession, ReadableSession {
    private final Socket socket;
    private String username;
    private final PrintWriter printWriter;
    private final BufferedReader bufferedReader;

    public ClientSession(Socket clientSocket, String username) throws IOException {
        this.socket = clientSocket;
        this.username = username;
        this.printWriter = new PrintWriter(socket.getOutputStream());
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PrintWriter getPrintWriter() {
        return printWriter;
    }

    public BufferedReader getBufferedReader() {
        return bufferedReader;
    }

    public void closeConnection() throws IOException {
        socket.close();
    }

    public boolean isConnectionClosed() {
        return socket.isClosed();
    }
}
