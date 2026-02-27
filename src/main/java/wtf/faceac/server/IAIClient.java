

package wtf.faceac.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IAIClient {
    CompletableFuture<Boolean> connect();

    CompletableFuture<Boolean> connectWithRetry();

    CompletableFuture<Void> disconnect();

    io.reactivex.rxjava3.core.Observable<AIResponse> predict(byte[] playerData, String playerUuid, String playerName);

    default CompletableFuture<AIResponse> predictAsync(byte[] playerData, String playerUuid, String playerName) {
        CompletableFuture<AIResponse> future = new CompletableFuture<>();
        AtomicBoolean emitted = new AtomicBoolean(false);
        predict(playerData, playerUuid, playerName).subscribe(
                response -> {
                    if (emitted.compareAndSet(false, true)) {
                        future.complete(response);
                    }
                },
                future::completeExceptionally,
                () -> {
                    if (emitted.compareAndSet(false, true)) {
                        future.completeExceptionally(new IllegalStateException("Empty AI response stream"));
                    }
                });
        return future;
    }

    boolean isConnected();

    boolean isLimitExceeded();

    String getSessionId();

    String getServerAddress();
}
