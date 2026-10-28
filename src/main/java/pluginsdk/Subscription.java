package pluginsdk;

import java.util.concurrent.CompletionStage;

/**
 * A call data subscription that can be closed and observed until it ends. Returned by the
 * subscription methods of {@link PluginClient}.
 *
 * @since 0.1.0
 */
public interface Subscription extends AutoCloseable {
  CompletionStage<SubscriptionEnd> completion();

  @Override
  void close();
}
