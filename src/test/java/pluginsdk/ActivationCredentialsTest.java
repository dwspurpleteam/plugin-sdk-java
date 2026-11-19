package pluginsdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import pluginsdk.internal.proto.EventServiceGrpc;
import pluginsdk.internal.proto.SubscribeEventsRequest;
import pluginsdk.internal.proto.SubscribeEventsResponse;

class ActivationCredentialsTest {
  @Test
  void sendsActivationIdAndSecretForSubscription() throws Exception {
    UUID activation = UUID.randomUUID();
    AtomicReference<String> authorization = new AtomicReference<>();
    AtomicReference<SubscribeEventsRequest> request = new AtomicReference<>();
    CountDownLatch received = new CountDownLatch(1);
    var service =
        new EventServiceGrpc.EventServiceImplBase() {
          @Override
          public void subscribe(
              SubscribeEventsRequest value, StreamObserver<SubscribeEventsResponse> observer) {
            request.set(value);
            received.countDown();
            observer.onCompleted();
          }
        };
    Server server =
        ServerBuilder.forPort(0)
            .addService(
                ServerInterceptors.intercept(
                    service,
                    new ServerInterceptor() {
                      @Override
                      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                          ServerCall<ReqT, RespT> call,
                          Metadata headers,
                          ServerCallHandler<ReqT, RespT> next) {
                        authorization.set(
                            headers.get(
                                Metadata.Key.of(
                                    "authorization", Metadata.ASCII_STRING_MARSHALLER)));
                        return next.startCall(call, headers);
                      }
                    }))
            .build()
            .start();
    try (var client =
        PluginClient.builder("localhost:" + server.getPort())
            .plaintext()
            .activation(activation, "secret")
            .build()) {
      client.subscribeEvents("call", EventFilter.all(), event -> {});
      assertTrue(received.await(5, TimeUnit.SECONDS));
      assertEquals(activation.toString(), request.get().getActivationId());
      assertEquals("Bearer secret", authorization.get());
    } finally {
      server.shutdownNow();
    }
  }
}
