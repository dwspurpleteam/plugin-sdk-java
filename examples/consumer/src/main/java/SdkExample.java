import java.util.concurrent.CountDownLatch;
import pluginsdk.EventFilter;
import pluginsdk.MediaFilter;
import pluginsdk.PluginClient;

@SuppressWarnings("PMD.CloseResource")
public class SdkExample {
  private static final int ARGS_WITH_ROOM = 3;

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
      var events = client.subscribeEvents(args[1], EventFilter.all(), new ExampleEventListener());

      events
          .completion()
          .whenComplete(
              (ignored, error) -> {
                if (error != null) System.err.println("events stopped: " + error);
              });

      if (args.length == ARGS_WITH_ROOM) {
        var media =
            client.subscribeMedia(args[1], args[2], MediaFilter.all(), new ExampleMediaListener());

        media
            .completion()
            .whenComplete(
                (ignored, error) -> {
                  if (error != null) System.err.println("media stopped: " + error);
                });
      }

      new CountDownLatch(1).await();
    }
  }
}
