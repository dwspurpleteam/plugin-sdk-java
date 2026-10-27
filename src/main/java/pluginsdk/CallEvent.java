package pluginsdk;

import java.time.Instant;

/** An event from the call platform */
public final class CallEvent {
    private final String id;
    private final String type;
    private final String source;
    private final String callId;
    private final String roomName;
    private final String participantId;
    private final String trackId;
    private final Instant occurredAt;

    CallEvent(String id, String type, String source, String callId, String roomName,
              String participantId, String trackId, Instant occurredAt) {
        this.id = id;
        this.type = type;
        this.source = source;
        this.callId = callId;
        this.roomName = roomName;
        this.participantId = participantId;
        this.trackId = trackId;
        this.occurredAt = occurredAt;
    }

    public String id() { return id; }
    public String type() { return type; }
    public String source() { return source; }
    public String callId() { return callId; }
    public String roomName() { return roomName; }
    public String participantId() { return participantId; }
    public String trackId() { return trackId; }
    /** Returns the event time, or null if the server omitted it */
    public Instant occurredAt() { return occurredAt; }
}
