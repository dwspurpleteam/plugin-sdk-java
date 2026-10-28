package pluginsdk;

/**
 * Receives call events and event subscription lifecycle notifications.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface EventListener extends StreamListener {
  void onEvent(CallEvent event);
}
