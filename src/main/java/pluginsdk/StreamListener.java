package pluginsdk;

/** Lifecycle callbacks are sequential within a subscription */
public interface StreamListener {
    /** The server sent response headers */
    default void onConnected() {}

    /** A temporary error interrupted the stream, a reconnect will be attempted */
    default void onDisconnected(StreamFailure failure) {}

    /** The subscription ended with an error and will not be retried */
    default void onFailed(StreamFailure failure) {}
}
