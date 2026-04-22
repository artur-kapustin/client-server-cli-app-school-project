package domain.server.game;

import java.util.concurrent.ConcurrentHashMap;

public class CoinTossGamesManager {
    private final ConcurrentHashMap<String, CoinTossGame> games = new ConcurrentHashMap<>();

    public void addGame(String gameId, String player1, String player2, int winScore) {
        games.put(gameId, new CoinTossGame(player1, player2, winScore));
    }

    public void removeGame(String gameId) {
        games.remove(gameId);
    }

    public CoinTossGame getGame(String gameId) {
        return games.get(gameId);
    }
}
