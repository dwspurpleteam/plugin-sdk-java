package pluginsdk;

/**
 * Receives RTP packets, track notifications, and media subscription lifecycle notifications.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface MediaListener extends StreamListener {
  void onPacket(MediaPacket packet);

  default void onTrack(TrackInfo track) {}

  default void onTrackEnded(TrackInfo track) {}
}
