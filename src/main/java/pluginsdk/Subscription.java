package pluginsdk;

import java.util.concurrent.CompletionStage;

/** An independent stream of call data */
public interface Subscription extends AutoCloseable {
    /** Completes when the subscription ends, exceptionally on failure */
    CompletionStage<SubscriptionEnd> completion();

    /** Cancels the stream and pending reconnect attempts */
    @Override
    void close();
}
