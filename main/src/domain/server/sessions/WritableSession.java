package domain.server.sessions;

import java.io.PrintWriter;

public interface WritableSession {
    PrintWriter getPrintWriter();
    String getUsername();
    void setUsername(String username);
}
