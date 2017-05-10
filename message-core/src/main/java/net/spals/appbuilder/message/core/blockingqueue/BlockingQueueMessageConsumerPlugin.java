package net.spals.appbuilder.message.core.blockingqueue;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.config.message.MessageConsumerConfig;
import net.spals.appbuilder.executor.core.ManagedExecutorService;
import net.spals.appbuilder.executor.core.ManagedExecutorServiceRegistry;
import net.spals.appbuilder.message.core.consumer.MessageConsumerCallback;
import net.spals.appbuilder.message.core.consumer.MessageConsumerPlugin;
import net.spals.appbuilder.model.core.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.spals.appbuilder.message.core.consumer.MessageConsumerCallback.unregisteredCallbackMessage;

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

    private final Set<MessageConsumerCallback<?>> consumerCallbackSet;
    private final BlockingQueue<BlockingQueueMessage> blockingMessageQueue;

    private final ManagedExecutorService executorService;

    private final Long pollTimeout;
    private final TimeUnit pollTimeoutUnit;

    @Inject
    BlockingQueueMessageConsumerPlugin(@ServiceConfig final Config serviceConfig,
                                       final Set<MessageConsumerCallback<?>> consumerCallbackSet,
                                       final ManagedExecutorServiceRegistry executorServiceRegistry,
                                       @Named("blockingMessageQueue") final BlockingQueue<BlockingQueueMessage> blockingMessageQueue) {
        this.pollTimeout = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("blockingQueue.messageConsumer.pollTimeout"))
                .map(config -> config.getLong("blockingQueue.messageConsumer.pollTimeout")).orElse(10L);
        this.pollTimeoutUnit = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("blockingQueue.messageConsumer.pollTimeoutUnit"))
                .map(config -> config.getEnum(TimeUnit.class, "blockingQueue.messageConsumer.pollTimeoutUnit")).orElse(TimeUnit.MILLISECONDS);

        this.consumerCallbackSet = consumerCallbackSet;
        this.blockingMessageQueue = blockingMessageQueue;
        // The number of registered consumer callbacks provides an upper bound on
        // the number of executor threads that we'll need.
        this.executorService = executorServiceRegistry.registerExecutorService(getClass(),
                Executors.newFixedThreadPool(Math.max(consumerCallbackSet.size(), 1)));
    }

    @Override
    public synchronized void start(final MessageConsumerConfig consumerConfig, final ModelSerializer modelSerializer) {
        final Map<Class<?>, MessageConsumerCallback<?>> consumerCallbacks =
                MessageConsumerCallback.loadCallbacksForTag(consumerConfig.getTag(), consumerCallbackSet);

        final Runnable consumerRunnable =
                new BlockingQueueConsumerRunnable(consumerCallbacks, consumerConfig, modelSerializer);
        executorService.submit(consumerRunnable);
    }

    @Override
    public void stop(final MessageConsumerConfig consumerConfig) {
        executorService.stop();
    }

    class BlockingQueueConsumerRunnable implements Runnable {

        private final Map<Class<?>, MessageConsumerCallback<?>> consumerCallbacks;
        private final MessageConsumerConfig consumerConfig;
        private final ModelSerializer modelSerializer;

        BlockingQueueConsumerRunnable(final Map<Class<?>, MessageConsumerCallback<?>> consumerCallbacks,
                                      final MessageConsumerConfig consumerConfig,
                                      final ModelSerializer modelSerializer) {
            this.consumerCallbacks = consumerCallbacks;
            this.consumerConfig = consumerConfig;
            this.modelSerializer = modelSerializer;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    final BlockingQueueMessage message = blockingMessageQueue.poll(pollTimeout, pollTimeoutUnit);
                    if (message != null) {
                        final Object payload = modelSerializer.deserialize(message.getSerializedPayload());
                        LOGGER.trace("Received '{}' message: {}", message.getTag(), payload);
                        final Optional<MessageConsumerCallback> consumerCallback =
                                Optional.ofNullable(consumerCallbacks.get(payload.getClass()));
                        if (consumerCallback.isPresent()) {
                            consumerCallback.get().processMessage(consumerConfig, payload);
                        } else {
                            LOGGER.warn(unregisteredCallbackMessage(consumerConfig.getTag(), payload.getClass()));
                        }
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
