package net.spals.appbuilder.message.core.blockingqueue;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.config.ConsumerConfig;
import net.spals.appbuilder.executor.core.ManagedExecutorService;
import net.spals.appbuilder.executor.core.ManagedExecutorServiceRegistry;
import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback;
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin;
import net.spals.appbuilder.message.core.formatter.MessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
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
@AutoBindInMap(baseClass = MessageConsumerPlugin.class, key = "blockingQueue")
class BlockingQueueMessageConsumerPlugin implements MessageConsumerPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingQueueMessageConsumerPlugin.class);

    private final Map<String, MessageConsumerCallback> consumerCallbackMap;
    private final BlockingQueue<BlockingQueueMessage> blockingMessageQueue;

    private final ManagedExecutorService executorService;

    private final Long pollTimeout;
    private final TimeUnit pollTimeoutUnit;

    @Inject
    BlockingQueueMessageConsumerPlugin(@ServiceConfig final Config serviceConfig,
                                       final Map<String, MessageConsumerCallback> consumerCallbackMap,
                                       final ManagedExecutorServiceRegistry executorServiceRegistry,
                                       @Named("blockingMessageQueue") final BlockingQueue<BlockingQueueMessage> blockingMessageQueue) {
        this.pollTimeout = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("blockingQueue.messageConsumer.pollTimeout"))
                .map(config -> config.getLong("blockingQueue.messageConsumer.pollTimeout")).orElse(10L);
        this.pollTimeoutUnit = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("blockingQueue.messageConsumer.pollTimeoutUnit"))
                .map(config -> config.getEnum(TimeUnit.class, "blockingQueue.messageConsumer.pollTimeoutUnit")).orElse(TimeUnit.MILLISECONDS);

        this.consumerCallbackMap = consumerCallbackMap;
        this.blockingMessageQueue = blockingMessageQueue;
        // The number of registered consumer callbacks provides an upper bound on
        // the number of executor threads that we'll need.
        this.executorService = executorServiceRegistry.registerExecutorService(getClass(),
                Executors.newFixedThreadPool(Math.max(consumerCallbackMap.size(), 1)));
    }

    @Override
    public synchronized void start(final ConsumerConfig consumerConfig, final MessageFormatter messageFormatter) {
        final MessageConsumerCallback consumerCallback = Optional.ofNullable(consumerCallbackMap.get(consumerConfig.getTag()))
                .orElseThrow(() -> new IllegalArgumentException(String.format("No MessageConsumerCallback for '%s' configuration", consumerConfig.getTag())));

        final Runnable consumerRunnable = new BlockingQueueConsumerRunnable(consumerConfig, messageFormatter, consumerCallback);
        executorService.submit(consumerRunnable);
    }

    @Override
    public void stop(final ConsumerConfig consumerConfig) {
        executorService.stop();
    }

    class BlockingQueueConsumerRunnable implements Runnable {

        private final ConsumerConfig consumerConfig;
        private final MessageConsumerCallback consumerCallback;
        private final MessageFormatter messageFormatter;

        BlockingQueueConsumerRunnable(final ConsumerConfig consumerConfig,
                                      final MessageFormatter messageFormatter,
                                      final MessageConsumerCallback consumerCallback) {
            this.consumerConfig = consumerConfig;
            this.consumerCallback = consumerCallback;
            this.messageFormatter = messageFormatter;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    final BlockingQueueMessage message = blockingMessageQueue.poll(pollTimeout, pollTimeoutUnit);
                    if (message != null) {
                        final Map<String, Object> payload = messageFormatter.deserializePayload(message.getSerializedPayload());
                        LOGGER.trace("Received '{}' message: {}", message.getTag(), payload);
                        consumerCallback.processMessage(consumerConfig, payload);
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.warn("BlockingQueue message queue was interrupted!");
            } catch (Throwable t) {
                LOGGER.error("Encountered unexpected error during callback of BlockingQueue messages", t);
            }

            LOGGER.info("Stopping blocking queue message consumer thread");
        }
    }
}
