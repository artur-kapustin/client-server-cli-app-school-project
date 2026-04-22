package domain.server.game;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CoinTossGame {
    private static final Random RANDOM = new Random();
    private final String player1;
    private final String player2;
    private final Map<String, Integer> scores;
    private final Map<String, Boolean> choices;
    private final int winScore;

    public CoinTossGame(String player1, String player2, int winScore) {
        this.player1 = player1;
        this.player2 = player2;

        this.scores = new HashMap<>();
        scores.put(player1, 0);
        scores.put(player2, 0);

        this.choices = new HashMap<>();
        choices.put(player1, null);
        choices.put(player2, null);

        this.winScore = winScore;
    }

    public synchronized boolean hasWon(String player) {
        return scores.get(player) == winScore;
    }

    public synchronized Map<String, Integer> putChoice(String player, boolean choice)
            throws InterruptedException {

        if (!choices.containsKey(player)) {
            throw new IllegalArgumentException("Unknown player.");
        }

        choices.put(player, choice);

        if (choices.get(player1) != null && choices.get(player2) != null) {
            toss();
            resetChoices();
            notifyAll();
            return new HashMap<>(scores);
        }

        while (choices.get(player1) != null || choices.get(player2) != null) {
            wait();
        }

        return new HashMap<>(scores);
    }

    public synchronized String getPlayer1() {
        return player1;
    }

    public synchronized String getPlayer2() {
        return player2;
    }

    public synchronized boolean isFinished() {
        return scores.containsValue(winScore);
    }

    private void toss() {
        boolean choice1 = choices.get(player1);
        boolean choice2 = choices.get(player2);

        if (choice1 != choice2) {
            boolean result = RANDOM.nextBoolean();
            if (choice1 == result) {
                increment(player1);
            } else {
                increment(player2);
            }
        }
    }

    private void increment(String player) {
        scores.put(player, scores.get(player) + 1);
    }

    private void resetChoices() {
        choices.replaceAll((_, _) -> null);
    }
}
