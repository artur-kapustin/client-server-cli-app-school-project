package domain.utils.messages;

public record TransferFile(String username, String transferId) implements Message {
}
