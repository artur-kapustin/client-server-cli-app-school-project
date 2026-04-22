package domain.utils.messages;

public record PrivateReq(String message, String username) implements Message {
}
