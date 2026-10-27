package pluginsdk;

@FunctionalInterface
public interface EventListener extends StreamListener {
  void onEvent(CallEvent event);
}
