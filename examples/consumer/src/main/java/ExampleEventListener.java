import pluginsdk.CallEvent;
import pluginsdk.EventListener;
import pluginsdk.StreamFailure;

final class ExampleEventListener implements EventListener {
  @Override
  public void onConnected() {
    System.out.println("events connected");
  }

  @Override
  public void onEvent(CallEvent event) {
    System.out.printf(
        "event %s participant=%s track=%s%n", event.type(), event.participantId(), event.trackId());
  }

  @Override
  public void onDisconnected(StreamFailure failure) {
    System.err.println("events disconnected, retrying: " + failure.code());
  }
}
