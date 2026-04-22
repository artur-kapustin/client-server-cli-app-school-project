package domain.utils.messages;

public record LogonResp(String status, int code) implements Message {}
