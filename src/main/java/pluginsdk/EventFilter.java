package pluginsdk;

import java.util.List;
import java.util.Objects;

/**
 * Selects event types and an optional participant for an event subscription. Used by {@link
 * PluginClient#subscribeEvents(String, EventFilter, EventListener)}.
 *
 * @since 0.1.0
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class EventFilter {
  private final String participantId;
  private final List<String> eventTypes;

  private EventFilter(String participantId, List<String> eventTypes) {
    this.participantId = participantId;
    this.eventTypes = List.copyOf(eventTypes);
  }

  public static EventFilter all() {
    return new EventFilter("", List.of());
  }

  public static EventFilter of(String... types) {
    return new EventFilter("", List.of(types));
  }

  public EventFilter withParticipant(String id) {
    return new EventFilter(Objects.requireNonNull(id, "id"), eventTypes);
  }

  public String participantId() {
    return participantId;
  }

  public List<String> eventTypes() {
    return eventTypes;
  }
}
