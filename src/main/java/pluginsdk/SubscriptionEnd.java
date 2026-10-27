package pluginsdk;

/** Reason for a successful subscription termination */
public enum SubscriptionEnd {
    /** The application closed the subscription or the client */
    CLOSED,
    /** The server completed the stream with OK */
    REMOTE_COMPLETED
}
