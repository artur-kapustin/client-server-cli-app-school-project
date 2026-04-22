package domain.utils.messages;

public record Broadcast(String username, String message) implements Message {}
