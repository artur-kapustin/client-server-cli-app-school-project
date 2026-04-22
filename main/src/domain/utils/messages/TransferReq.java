package domain.utils.messages;

public record TransferReq(String receiver, String filename, String checksum) implements Message {
}
