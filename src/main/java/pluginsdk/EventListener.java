package pluginsdk;

/** Receives events and lifecycle notifications for one subscription */
@FunctionalInterface
public interface EventListener extends StreamListener {
    void onEvent(CallEvent event);
}
