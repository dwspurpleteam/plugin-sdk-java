package pluginsdk;

import java.util.Objects;
import java.util.Set;

public final class MediaFilter {
    private final String participantId;
    private final String trackId;
    private final Set<MediaKind> kinds;

    private MediaFilter(String participantId, String trackId, Set<MediaKind> kinds) {
        this.participantId = participantId;
        this.trackId = trackId;
        this.kinds = Set.copyOf(kinds);
    }

    public static MediaFilter all() { return new MediaFilter("", "", Set.of()); }
    public static MediaFilter audio() { return new MediaFilter("", "", Set.of(MediaKind.AUDIO)); }
    public static MediaFilter video() { return new MediaFilter("", "", Set.of(MediaKind.VIDEO)); }
    public MediaFilter withParticipant(String id) { return new MediaFilter(Objects.requireNonNull(id, "id"), trackId, kinds); }
    public MediaFilter withTrack(String id) { return new MediaFilter(participantId, Objects.requireNonNull(id, "id"), kinds); }
    public String participantId() { return participantId; }
    public String trackId() { return trackId; }
    public Set<MediaKind> kinds() { return kinds; }
}
