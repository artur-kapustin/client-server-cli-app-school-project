package domain.utils.messages;

import java.util.Map;

// Map<username, score>
public record CoinTossResult(Map<String, Integer> scores) implements Message {
}
