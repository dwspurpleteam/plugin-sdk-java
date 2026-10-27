package pluginsdk;

/** Receives media packets and track lifecycle notifications */
@FunctionalInterface
public interface MediaListener extends StreamListener {
    void onPacket(MediaPacket packet);

    /** A track is reported before its first received packet, not necessarily when it was published */
    default void onTrack(TrackInfo track) {}

    default void onTrackEnded(TrackInfo track) {}
}
