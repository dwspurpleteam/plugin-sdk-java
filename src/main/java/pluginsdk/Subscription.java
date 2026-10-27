package pluginsdk;

import java.util.concurrent.CompletionStage;

public interface Subscription extends AutoCloseable {
  CompletionStage<SubscriptionEnd> completion();

  @Override
  void close();
}
