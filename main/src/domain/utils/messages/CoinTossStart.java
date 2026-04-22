package domain.utils.messages;

public record CoinTossStart(String username, String gameId) implements Message {
}
