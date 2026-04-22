package domain.utils.constants;

import java.util.HashMap;
import java.util.Map;

public class Codes {
    public static final int OK = 0;
    public static final int INVALID_TRANSFER_ID = 4000;
    public static final int USERNAME_ALREADY_EXISTS = 5000;
    public static final int USERNAME_HAS_INVALID_FORMAT_OR_LENGTH = 5001;
    public static final int ALREADY_LOGGED_IN = 5002;
    public static final int NOT_LOGGED_IN = 6000;
    public static final int NON_EXISTENT_USERNAME_PROVIDED = 6001;
    public static final int NO_PONG = 7000;
    public static final int PONG_NO_PING = 8000;
    public static final int INVALID_GAME_ID = 9000;

    private static final HashMap<Integer, String> codes = new HashMap<>(Map.of(
            OK, "OK",
            INVALID_TRANSFER_ID, "Invalid answer format. Must be 0 or 1.",
            USERNAME_ALREADY_EXISTS, "User with this name already exists.",
            USERNAME_HAS_INVALID_FORMAT_OR_LENGTH, "Username has an invalid format or length.",
            ALREADY_LOGGED_IN, "Already logged in.",
            NOT_LOGGED_IN, "User is not logged in.",
            NON_EXISTENT_USERNAME_PROVIDED, "Non existent username provided.",
            NO_PONG, "No pong received.",
            PONG_NO_PING, "Pong without ping.",
            INVALID_GAME_ID, "Invalid game id. (You probably have not started a game)"
    ));

    public static String get(int code) {
        return codes.get(code);
    }
}
