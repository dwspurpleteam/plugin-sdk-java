package pluginsdk;

import java.io.IOException;

/** An action rejected or failed by the plugin manager. */
public final class ActionException extends IOException {
  private static final long serialVersionUID = 1L;
  private final int status;
  private final String body;

  public ActionException(int statusCode, String responseBody) {
    super("Plugin action failed with HTTP " + statusCode + ": " + responseBody);
    this.status = statusCode;
    this.body = responseBody;
  }

  public int statusCode() {
    return status;
  }

  public String responseBody() {
    return body;
  }
}
