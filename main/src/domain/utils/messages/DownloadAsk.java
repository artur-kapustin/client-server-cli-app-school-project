package domain.utils.messages;

public record DownloadAsk(String sender, String filename, String checksum, String transferId) implements Message {
}
