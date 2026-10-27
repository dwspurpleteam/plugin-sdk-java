package pluginsdk;

@FunctionalInterface
public interface MediaListener extends StreamListener {
  void onPacket(MediaPacket packet);

  default void onTrack(TrackInfo track) {}

  default void onTrackEnded(TrackInfo track) {}
}
