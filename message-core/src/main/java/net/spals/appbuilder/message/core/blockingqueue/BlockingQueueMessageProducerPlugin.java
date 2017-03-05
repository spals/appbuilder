package net.spals.appbuilder.message.core.blockingqueue;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.config.ProducerConfig;
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@link MessageProducerPlugin} for producing messages
 * to a local {@link BlockingQueue}.
 *
 * This is useful for testing purposes.
 * Not to be used in a Production environment.
 *
 * @author tkral
 */
@AutoBindInMap(baseClass = MessageProducerPlugin.class, key = "blockingQueue")
class BlockingQueueMessageProducerPlugin implements MessageProducerPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingQueueMessageProducerPlugin.class);

    private final BlockingQueue<BlockingQueueMessage> blockingMessageQueue;

    private final Optional<Long> offerTimeout;
    private final Optional<TimeUnit> offerTimeoutUnit;

    @Inject
    BlockingQueueMessageProducerPlugin(@ServiceConfig final Config serviceConfig,
                                       @Named("blockingMessageQueue") final BlockingQueue<BlockingQueueMessage> blockingMessageQueue) {
        this.blockingMessageQueue = blockingMessageQueue;
        this.offerTimeout = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("blockingQueue.messageProducer.offerTimeout"))
                .map(config -> config.getLong("blockingQueue.messageProducer.offerTimeout"));
        this.offerTimeoutUnit = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("blockingQueue.messageProducer.offerTimeoutUnit"))
                .map(config -> config.getEnum(TimeUnit.class, "blockingQueue.messageProducer.offerTimeoutUnit"));
        checkState(offerTimeout.isPresent() == offerTimeoutUnit.isPresent(),
                "blockingQueue.messageProducer.offerTimeout and blockingQueue.messageProducer.offerTimeoutUnit must both have values");
    }

    @Override
    public void sendMessage(final ProducerConfig producerConfig,
                            final byte[] serializedPayload) throws IOException {
        final BlockingQueueMessage message = new BlockingQueueMessage.Builder()
                .setSerializedPayload(serializedPayload).setTag(producerConfig.getTag()).build();
        try {
            if (offerTimeout.isPresent()) {
                blockingMessageQueue.offer(message, offerTimeout.get(), offerTimeoutUnit.get());
            } else {
                blockingMessageQueue.offer(message);
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interupted while sending message on blcoking queue", e);
        }
    }
}
