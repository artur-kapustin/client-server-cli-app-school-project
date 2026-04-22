package domain.server;

import domain.server.sessions.ClientSession;
import domain.server.sessions.WritableSession;
import domain.utils.constants.StatusCodes;
import domain.utils.messages.ByeResp;
import domain.utils.messages.Hi;
import domain.utils.messages.Left;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientSessionManager implements Runnable {
    private final Map<String, ClientSession> sessions;
    private final ClientSession clientSession;
    private final ServerMessageManager serverMessageManager;
    private final PingPongHandler pingPongHandler;
    private final Listener listener;

    public ClientSessionManager(Socket socket, Map<String, ClientSession> sessions) throws IOException {
        this.clientSession = new ClientSession(socket, "");
        this.sessions = sessions;
        this.serverMessageManager = new ServerMessageManager(
                clientSession,
                this::sessionsMapToWritableSessionsMap,
                this::onLogon
        );
        this.pingPongHandler = new PingPongHandler(
                serverMessageManager,
                this::closeConnection,
                clientSession::isConnectionClosed
        );
        this.listener = new Listener(
                clientSession,
                serverMessageManager,
                this::closeConnection,
                pingPongHandler::onPong
        );
        serverMessageManager.sendMessage(new Hi("1.7.0"));
    }

    @Override
    public void run() {
        listener.listenToClient();
    }

    private Map<String, WritableSession> sessionsMapToWritableSessionsMap() {
        synchronized (sessions) {
            Map<String, WritableSession> map = new HashMap<>();
            for (ClientSession c : sessions.values()) {
                map.put(c.getUsername(), c);
            }
            return map;
        }
    }

    private void closeConnection() {
        String username = clientSession.getUsername();

        serverMessageManager.sendMessage(new ByeResp(StatusCodes.OK, 0));
        serverMessageManager.broadcastMessage(new Left(username));
        sessions.remove(username);
        if (!clientSession.isConnectionClosed()) {
            try {
                clientSession.closeConnection();
                System.out.println("Closed connection.");
            } catch (IOException e) {
                System.err.println("Connection already closed.");
            }
        }
    }

    private void onLogon() {
        new Thread(pingPongHandler).start();
        sessions.put(clientSession.getUsername(), clientSession);
        System.out.println(clientSession);
    }
}
