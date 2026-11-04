package pluginsdk;

/** Describes a failed action with its streamapi error code. */
public final class ActionException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final String errorCode;

  public ActionException(String code, String message, Throwable cause) {
    super(message == null ? code : message, cause);
    this.errorCode = code;
  }

  public String code() {
    return errorCode;
  }
}
