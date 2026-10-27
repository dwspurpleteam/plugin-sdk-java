import java.util.concurrent.CountDownLatch;
import java.util.HashMap;
import pluginsdk.CallEvent;
import pluginsdk.EventFilter;
import pluginsdk.EventListener;
import pluginsdk.MediaFilter;
import pluginsdk.MediaListener;
import pluginsdk.MediaPacket;
import pluginsdk.PluginClient;
import pluginsdk.StreamFailure;
import pluginsdk.TrackInfo;

/** Subscribe to events and optionally RTP using the published SDK */
public class SdkExample {
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: SdkExample <target> <call-id> [room-name]");
            System.exit(2);
        }
        var builder = PluginClient.builder(args[0]);
        if (Boolean.parseBoolean(System.getenv("PLUGIN_SDK_PLAINTEXT"))) builder.plaintext();
        String cookie = System.getenv("PLUGIN_SDK_COOKIE");
        if (cookie != null && !cookie.isBlank()) builder.metadata("cookie", cookie);
        try (var client = builder.build()) {
            var events = client.subscribeEvents(args[1], EventFilter.all(), new EventListener() {
                @Override public void onConnected() { System.out.println("events connected"); }
                @Override public void onEvent(CallEvent event) {
                    System.out.printf("event %s participant=%s track=%s%n", event.type(), event.participantId(), event.trackId());
                }
                @Override public void onDisconnected(StreamFailure failure) {
                    System.err.println("events disconnected, retrying: " + failure.code());
                }
            });
            events.completion().whenComplete((ignored, error) -> {
                if (error != null) System.err.println("events stopped: " + error);
            });
            if (args.length == 3) {
                var packets = new HashMap<String, Long>();
                var media = client.subscribeMedia(args[1], args[2], MediaFilter.all(), new MediaListener() {
                    @Override public void onTrack(TrackInfo track) {
                        System.out.printf("track %s %s %s %s%n", track.trackId(), track.participantId(), track.kind(), track.mimeType());
                    }
                    @Override public void onPacket(MediaPacket packet) {
                        long count = packets.merge(packet.track().trackId(), 1L, Long::sum);
                        if (count == 1 || count % 100 == 0) {
                            System.out.printf("rtp track=%s seq=%d bytes=%d%n", packet.track().trackId(), packet.sequenceNumber(), packet.rtp().remaining());
                        }
                    }
                    @Override public void onTrackEnded(TrackInfo track) {
                        packets.remove(track.trackId());
                        System.out.println("ended " + track.trackId());
                    }
                    @Override public void onDisconnected(StreamFailure failure) {
                        packets.clear();
                        System.err.println("media disconnected, retrying: " + failure.code());
                    }
                });
                media.completion().whenComplete((ignored, error) -> {
                    if (error != null) System.err.println("media stopped: " + error);
                });
            }
            new CountDownLatch(1).await();
        }
    }
}
