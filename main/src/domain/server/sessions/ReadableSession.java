package domain.server.sessions;

import java.io.BufferedReader;

public interface ReadableSession {
    BufferedReader getBufferedReader();
    String getUsername();
}
