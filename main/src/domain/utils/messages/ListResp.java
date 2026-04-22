package domain.utils.messages;

import java.util.*;

public record ListResp(String status, List<String> usernames) implements Message {
}
