package domain.server;

import domain.utils.constants.Codes;
import domain.utils.messages.Hangup;
import domain.utils.messages.Ping;
import domain.utils.messages.PongError;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class PingPongHandler implements Runnable {
    private final ServerMessageManager serverMessageManager;
    private CompletableFuture<Void> future;
    private final Runnable onTimeout;
    private final Supplier<Boolean> isConnectionClosed;

    public PingPongHandler(ServerMessageManager serverMessageManager, Runnable onTimeout, Supplier<Boolean> isConnectionClosed) {
        this.serverMessageManager = serverMessageManager;
        this.onTimeout = onTimeout;
        this.isConnectionClosed = isConnectionClosed;
    }

    @Override
    public void run() {
        AtomicBoolean isRunning = new AtomicBoolean(true);
        while (isRunning.get()) {
            if (isConnectionClosed.get()) {
                return;
            }

            try {
                Thread.sleep(10000);
                future = new CompletableFuture<Void>()
                        .orTimeout(2, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            if (ex instanceof TimeoutException) {
                                System.out.println("Heartbeat timed out!");
                                onTimeout.run();
                                serverMessageManager.sendMessage(new Hangup(Codes.NO_PONG));
                                isRunning.set(false);
                                future.complete(null);
                            } else {
                                System.out.println("Future completed exceptionally: " + ex);
                            }
                            return null;
                        });
                serverMessageManager.sendMessage(new Ping());
                System.out.println("S -> C Ping");

                future.get();
            } catch (ExecutionException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    public void onPong() {
        if (future != null && !future.isDone()) {
            future.complete(null);
            System.out.println("C -> S Pong");
        } else {
            serverMessageManager.sendMessage(new PongError(Codes.PONG_NO_PING));
        }
    }
}
