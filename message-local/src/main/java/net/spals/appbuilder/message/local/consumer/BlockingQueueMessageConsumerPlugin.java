package net.spals.appbuilder.message.local.consumer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.config.ConsumerConfig;
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin;
import net.spals.appbuilder.message.core.consumer.callback.MessageConsumerCallback;
import net.spals.appbuilder.message.core.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@link MessageConsumerPlugin} for consuming messages
 * from a local {@link BlockingQueue}.
 *
 * This is useful for testing purposes.
 * Not to be used in a Production environment.
 *
 * @author tkral
 */
@AutoBindInMap(baseClass = MessageConsumerPlugin.class, key = "local")
class BlockingQueueMessageConsumerPlugin implements MessageConsumerPlugin, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingQueueMessageConsumerPlugin.class);

    private final Map<String, MessageConsumerCallback> consumerCallbackMap;
    private final BlockingQueue<Message> localMessageQueue;
    private final ExecutorService executor;

    private final Long pollTimeout;
    private final TimeUnit pollTimeoutUnit;

    @Inject
    BlockingQueueMessageConsumerPlugin(@ServiceConfig final Config serviceConfig,
                                       final Map<String, MessageConsumerCallback> consumerCallbackMap,
                                       @Named("localMessageQueue") final BlockingQueue<Message> localMessageQueue) {
        this.pollTimeout = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("local.queue.pollTimeout"))
                .map(config -> config.getLong("local.queue.pollTimeout")).orElse(10L);
        this.pollTimeoutUnit = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("local.queue.pollTimeoutUnit"))
                .map(config -> config.getEnum(TimeUnit.class, "local.queue.pollTimeoutUnit")).orElse(TimeUnit.MILLISECONDS);

        this.consumerCallbackMap = consumerCallbackMap;
        this.localMessageQueue = localMessageQueue;
        // The number of registered consumer callbacks provides an upper bound on
        // the number of executor threads that we'll need.
        this.executor = Executors.newFixedThreadPool(consumerCallbackMap.size());
    }

    @Override
    public synchronized void start(final ConsumerConfig consumerConfig) {
        // 1. Verify that we have a matching callback for this configuration
        checkState(consumerCallbackMap.containsKey(consumerConfig.getTag()),
                "No MessageConsumerCallback for '%s' configuration", consumerConfig.getTag());
        executor.submit(this);
    }

    @Override
    public void stop(final ConsumerConfig consumerConfig) {
        // See https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(60L, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    LOGGER.error("Error encountered while shutting down local message queue executor");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                final Message message = localMessageQueue.poll(pollTimeout, pollTimeoutUnit);
                if (message != null) {
                    LOGGER.trace("Received '{}' message: {}", message.getTag(), message.getPayload());
                    final MessageConsumerCallback consumerCallback = consumerCallbackMap.get(message.getTag());
                    consumerCallback.processMessage(message);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Local message queue was interrupted!");
        } catch (Throwable t) {
            LOGGER.error("Encountered unexpected error during callback of local messages", t);
        }

        LOGGER.info("Stopping local message consumer thread");
    }
}
