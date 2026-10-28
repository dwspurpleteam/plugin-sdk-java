package pluginsdk;

/**
 * Receives connection and failure notifications for a subscription.
 *
 * @since 0.1.0
 */
public interface StreamListener {
  default void onConnected() {}

  default void onDisconnected(StreamFailure failure) {}

  default void onFailed(StreamFailure failure) {}
}
