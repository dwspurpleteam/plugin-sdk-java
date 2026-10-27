package pluginsdk;

public interface StreamListener {
  default void onConnected() {}

  default void onDisconnected(StreamFailure failure) {}

  default void onFailed(StreamFailure failure) {}
}
