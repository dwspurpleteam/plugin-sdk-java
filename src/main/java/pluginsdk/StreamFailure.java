package pluginsdk;

public final class StreamFailure extends RuntimeException {
    private final String code;

    public StreamFailure(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() { return code; }
}
