import java.util.HashMap;
import java.util.Map;
import pluginsdk.MediaListener;
import pluginsdk.MediaPacket;
import pluginsdk.StreamFailure;
import pluginsdk.TrackInfo;

final class ExampleMediaListener implements MediaListener {
  private final Map<String, Long> packets = new HashMap<>();

  @Override
  public void onTrack(TrackInfo track) {
    System.out.printf(
        "track %s %s %s %s%n",
        track.trackId(), track.participantId(), track.kind(), track.mimeType());
  }

  @Override
  public void onPacket(MediaPacket packet) {
    long count = packets.merge(packet.track().trackId(), 1L, Long::sum);
    if (count == 1 || count % 100 == 0) {
      System.out.printf(
          "rtp track=%s seq=%d bytes=%d%n",
          packet.track().trackId(), packet.sequenceNumber(), packet.rtp().remaining());
    }
  }

  @Override
  public void onTrackEnded(TrackInfo track) {
    packets.remove(track.trackId());
    System.out.println("ended " + track.trackId());
  }

  @Override
  public void onDisconnected(StreamFailure failure) {
    packets.clear();
    System.err.println("media disconnected, retrying: " + failure.code());
  }
}
