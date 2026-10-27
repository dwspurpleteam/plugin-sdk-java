package pluginsdk;

public final class TrackInfo {
    private final String trackId;
    private final String participantId;
    private final MediaKind kind;
    private final String mimeType;
    private final long clockRate;
    private final long channels;
    private final String fmtp;

    TrackInfo(String trackId, String participantId, MediaKind kind, String mimeType,
              long clockRate, long channels, String fmtp) {
        this.trackId = trackId;
        this.participantId = participantId;
        this.kind = kind;
        this.mimeType = mimeType;
        this.clockRate = clockRate;
        this.channels = channels;
        this.fmtp = fmtp;
    }

    public String trackId() { return trackId; }
    public String participantId() { return participantId; }
    public MediaKind kind() { return kind; }
    public String mimeType() { return mimeType; }
    public long clockRate() { return clockRate; }
    public long channels() { return channels; }
    public String fmtp() { return fmtp; }
}
