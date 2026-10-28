package pluginsdk;

/**
 * Describes a subscription failure with its streamapi or SDK error code. Delivered to {@link
 * StreamListener#onFailed(StreamFailure)} or through {@link Subscription#completion()}.
 *
 * @since 0.1.0
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class StreamFailure extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final String code;

  public StreamFailure(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
