package pluginsdk;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Actions available to an active plugin in a call. */
public final class PluginActions {
  private static final int NO_CONTENT = 204;
  private final HttpClient client;
  private final URI manager;
  private final UUID activationId;
  private final Map<String, String> headers;

  public PluginActions(URI manager, UUID activationId) {
    this(manager, activationId, Map.of());
  }

  public PluginActions(URI manager, UUID activationId, Map<String, String> headers) {
    this.manager = Objects.requireNonNull(manager, "manager");
    if (!("http".equals(manager.getScheme()) || "https".equals(manager.getScheme()))
        || manager.getHost() == null
        || manager.getRawQuery() != null
        || manager.getRawFragment() != null) {
      throw new IllegalArgumentException("manager must be an HTTP URL without a query or fragment");
    }
    this.activationId = Objects.requireNonNull(activationId, "activationId");
    this.headers = Map.copyOf(headers);
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  /** Mutes the participant's published microphone tracks. */
  public void muteMicrophone(String participantId) throws IOException, InterruptedException {
    if (participantId == null || participantId.isBlank()) {
      throw new IllegalArgumentException("participantId must not be blank");
    }
    String participant =
        URLEncoder.encode(participantId, StandardCharsets.UTF_8).replace("+", "%20");
    String base = manager.toString().replaceAll("/+$", "");
    URI url =
        URI.create(
            base
                + "/v1/activations/"
                + activationId
                + "/actions/participants/"
                + participant
                + "/microphone/mute");
    HttpRequest.Builder request =
        HttpRequest.newBuilder(url)
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.noBody());
    headers.forEach(request::header);
    HttpResponse<String> response =
        client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != NO_CONTENT) {
      throw new ActionException(response.statusCode(), response.body());
    }
  }
}
