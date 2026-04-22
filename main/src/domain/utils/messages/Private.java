package domain.utils.messages;

public record Private(String senderUsername, String message) implements Message {
}
