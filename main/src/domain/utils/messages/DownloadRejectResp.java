package domain.utils.messages;

public record DownloadRejectResp(String status, int code) implements Message {
}
