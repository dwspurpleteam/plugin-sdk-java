package pluginsdk;

import com.google.protobuf.Timestamp;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import pluginsdk.internal.proto.EventServiceGrpc;
import pluginsdk.internal.proto.MediaServiceGrpc;
import pluginsdk.internal.proto.SubscribeEventsRequest;
import pluginsdk.internal.proto.SubscribeEventsResponse;
import pluginsdk.internal.proto.SubscribeRequest;
import pluginsdk.internal.proto.SubscribeResponse;

public final class PluginClient implements AutoCloseable {
    private static final Metadata.Key<String> CALL_ID = Metadata.Key.of("x-call-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final int MAX_RETRIES = 8;
    private final ManagedChannel channel;
    private final Map<String, String> metadata;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "plugin-sdk-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final Set<Stream<?>> streams = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    private PluginClient(ManagedChannel channel, Map<String, String> metadata) {
        this.channel = channel;
        this.metadata = Map.copyOf(metadata);
    }

    public static Builder builder(String target) { return new Builder(target); }

    static PluginClient forChannel(ManagedChannel channel, Map<String, String> metadata) {
        return new PluginClient(channel, metadata);
    }

    public Subscription subscribeEvents(String callId, EventFilter filter, EventListener listener) {
        requireCallId(callId);
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(listener, "listener");
        var request = SubscribeEventsRequest.newBuilder().setCallId(callId)
                .setParticipantId(filter.participantId()).addAllEventTypes(filter.eventTypes()).build();
        Stream<SubscribeEventsResponse> stream = new Stream<>(callId, listener,
                EventServiceGrpc.getSubscribeMethod(), request, response -> {
                    Timestamp t = response.getOccurredAt();
                    Instant time = response.hasOccurredAt() ? Instant.ofEpochSecond(t.getSeconds(), t.getNanos()) : null;
                    listener.onEvent(new CallEvent(response.getEventId(), response.getType(), response.getSource(),
                            response.getCallId(), response.getRoomName(), response.getParticipantId(),
                            response.getTrackId(), time));
                }, () -> {});
        return start(stream);
    }

    public Subscription subscribeMedia(String callId, String roomName, MediaFilter filter, MediaListener listener) {
        requireCallId(callId);
        if (roomName == null || roomName.isBlank()) throw new IllegalArgumentException("roomName must not be blank");
        Objects.requireNonNull(filter, "filter");
        Objects.requireNonNull(listener, "listener");
        var builder = SubscribeRequest.newBuilder().setCallId(callId).setRoomName(roomName)
                .setParticipantId(filter.participantId()).setTrackId(filter.trackId());
        for (MediaKind kind : filter.kinds()) {
            builder.addKinds(kind == MediaKind.AUDIO ? pluginsdk.internal.proto.MediaKind.MEDIA_KIND_AUDIO
                    : pluginsdk.internal.proto.MediaKind.MEDIA_KIND_VIDEO);
        }
        Map<Integer, TrackInfo> tracks = new HashMap<>();
        Consumer<SubscribeResponse> receive = response -> {
            switch (response.getPayloadCase()) {
                case TRACK -> {
                    var t = response.getTrack();
                    MediaKind kind = switch (t.getKind()) {
                        case MEDIA_KIND_AUDIO -> MediaKind.AUDIO;
                        case MEDIA_KIND_VIDEO -> MediaKind.VIDEO;
                        default -> throw new StreamFailure("PROTOCOL_ERROR", "unknown media kind", null);
                    };
                    var info = new TrackInfo(t.getTrackId(), t.getParticipantId(), kind, t.getMimeType(),
                            Integer.toUnsignedLong(t.getClockRate()), Integer.toUnsignedLong(t.getChannels()), t.getFmtp());
                    if (tracks.putIfAbsent(t.getHandle(), info) != null) {
                        throw new StreamFailure("PROTOCOL_ERROR", "duplicate track handle", null);
                    }
                    listener.onTrack(info);
                }
                case PACKET -> {
                    var packet = response.getPacket();
                    TrackInfo track = tracks.get(packet.getHandle());
                    if (track == null) throw new StreamFailure("PROTOCOL_ERROR", "unknown track handle", null);
                    long nanos = packet.getReceivedAtUnixNano();
                    Instant received = Instant.ofEpochSecond(Math.floorDiv(nanos, 1_000_000_000L),
                            Math.floorMod(nanos, 1_000_000_000L));
                    MediaPacket media;
                    try {
                        media = new MediaPacket(track, packet.getRtp().toByteArray(), received);
                    } catch (IllegalArgumentException e) {
                        throw new StreamFailure("PROTOCOL_ERROR", "invalid RTP packet", e);
                    }
                    listener.onPacket(media);
                }
                case ENDED -> {
                    TrackInfo track = tracks.remove(response.getEnded().getHandle());
                    if (track == null) throw new StreamFailure("PROTOCOL_ERROR", "unknown track handle", null);
                    listener.onTrackEnded(track);
                }
                case PAYLOAD_NOT_SET -> {}
            }
        };
        return start(new Stream<>(callId, listener, MediaServiceGrpc.getSubscribeMethod(),
                builder.build(), receive, tracks::clear));
    }

    private Subscription start(Stream<?> stream) {
        if (closed.get()) throw new IllegalStateException("client is closed");
        streams.add(stream);
        if (closed.get()) {
            stream.close();
            throw new IllegalStateException("client is closed");
        }
        stream.connect();
        return stream;
    }

    private static void requireCallId(String callId) {
        if (callId == null || callId.isBlank()) throw new IllegalArgumentException("callId must not be blank");
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        streams.forEach(Stream::close);
        scheduler.shutdownNow();
        channel.shutdownNow();
    }

    public static final class Builder {
        private final String target;
        private final Map<String, String> metadata = new HashMap<>();
        private boolean plaintext;

        private Builder(String target) {
            if (target == null || target.isBlank()) throw new IllegalArgumentException("target must not be blank");
            this.target = target;
        }

        public Builder plaintext() { plaintext = true; return this; }

        public Builder metadata(String key, String value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            if (key.equalsIgnoreCase("x-call-id")) throw new IllegalArgumentException("x-call-id is set per subscription");
            String normalized = key.toLowerCase(Locale.ROOT);
            Metadata.Key.of(normalized, Metadata.ASCII_STRING_MARSHALLER);
            metadata.put(normalized, value);
            return this;
        }

        public PluginClient build() {
            ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forTarget(target);
            if (plaintext) builder.usePlaintext(); else builder.useTransportSecurity();
            return new PluginClient(builder.build(), metadata);
        }
    }

    private final class Stream<T> implements Subscription {
        private final Object lock = new Object();
        private final String callId;
        private final StreamListener listener;
        private final MethodDescriptor<?, T> method;
        private final Object request;
        private final Consumer<T> receive;
        private final Runnable reset;
        private final CompletableFuture<SubscriptionEnd> completion = new CompletableFuture<>();
        private ClientCall<Object, T> call;
        private ScheduledFuture<?> retry;
        private boolean stopped;
        private int failures;
        private long connectedAt;

        Stream(String callId, StreamListener listener, MethodDescriptor<?, T> method, Object request,
               Consumer<T> receive, Runnable reset) {
            this.callId = callId;
            this.listener = listener;
            this.method = method;
            this.request = request;
            this.receive = receive;
            this.reset = reset;
        }

        @SuppressWarnings("unchecked")
        void connect() {
            synchronized (lock) {
                if (stopped) return;
                retry = null;
                Metadata headers = new Metadata();
                metadata.forEach((key, value) -> headers.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));
                headers.put(CALL_ID, callId);
                ClientCall<Object, T> current = channel.newCall((MethodDescriptor<Object, T>) method, CallOptions.DEFAULT);
                call = current;
                current.start(new ClientCall.Listener<>() {
                    @Override public void onHeaders(Metadata ignored) {
                        synchronized (lock) {
                            if (stopped || call != current) return;
                            connectedAt = System.nanoTime();
                        }
                        invoke(listener::onConnected);
                    }
                    @Override public void onMessage(T value) {
                        if (!isCurrent(current)) return;
                        if (invoke(() -> receive.accept(value))) {
                            synchronized (lock) {
                                if (!stopped && call == current) current.request(1);
                            }
                        }
                    }
                    @Override public void onClose(Status status, Metadata ignored) { ended(current, status); }
                }, headers);
                current.sendMessage(request);
                current.halfClose();
                current.request(1);
            }
        }

        private boolean isCurrent(ClientCall<Object, T> candidate) {
            synchronized (lock) { return !stopped && call == candidate; }
        }

        private boolean invoke(Runnable action) {
            try { action.run(); return true; }
            catch (StreamFailure e) { fail(e); return false; }
            catch (RuntimeException e) {
                fail(new StreamFailure("CALLBACK_ERROR", "subscription handler failed", e));
                return false;
            }
        }

        private void ended(ClientCall<Object, T> current, Status status) {
            synchronized (lock) {
                if (stopped || call != current) return;
                call = null;
                if (connectedAt != 0 && System.nanoTime() - connectedAt >= TimeUnit.SECONDS.toNanos(30)) failures = 0;
                connectedAt = 0;
            }
            reset.run();
            if (status.isOk()) { finish(null, SubscriptionEnd.REMOTE_COMPLETED); return; }
            StreamFailure error = new StreamFailure(status.getCode().name(),
                    status.getDescription() == null ? status.getCode().name() : status.getDescription(), status.getCause());
            if (status.getCode() != Status.Code.UNAVAILABLE) { fail(error); return; }
            boolean exhausted;
            synchronized (lock) {
                if (stopped) return;
                exhausted = failures >= MAX_RETRIES;
            }
            if (exhausted) { fail(error); return; }
            try { listener.onDisconnected(error); }
            catch (RuntimeException e) { fail(new StreamFailure("CALLBACK_ERROR", "disconnect handler failed", e)); return; }
            synchronized (lock) {
                if (stopped) return;
                long base = Math.min(250L << failures++, 5000L);
                long delay = Math.max(1, (long) (base * ThreadLocalRandom.current().nextDouble(0.8, 1.2)));
                retry = scheduler.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
            }
        }

        private void fail(StreamFailure error) {
            if (finish(error, null)) {
                try { listener.onFailed(error); } catch (RuntimeException ignored) {}
            }
        }

        private boolean finish(StreamFailure error, SubscriptionEnd end) {
            ClientCall<Object, T> current;
            synchronized (lock) {
                if (stopped) return false;
                stopped = true;
                if (retry != null) retry.cancel(false);
                current = call;
                call = null;
            }
            if (current != null) current.cancel("subscription closed", error);
            streams.remove(this);
            if (error == null) completion.complete(end); else completion.completeExceptionally(error);
            return true;
        }

        @Override public java.util.concurrent.CompletionStage<SubscriptionEnd> completion() { return completion.minimalCompletionStage(); }
        @Override public void close() { finish(null, SubscriptionEnd.CLOSED); }
    }
}
