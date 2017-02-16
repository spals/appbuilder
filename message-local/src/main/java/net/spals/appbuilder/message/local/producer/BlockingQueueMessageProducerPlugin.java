package net.spals.appbuilder.message.local.producer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.message.core.model.Message;
import net.spals.appbuilder.message.core.producer.MessageProducerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@AutoBindInMap(baseClass = MessageProducerPlugin.class, key = "local")
class BlockingQueueMessageProducerPlugin implements MessageProducerPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockingQueueMessageProducerPlugin.class);

    private final BlockingQueue<Message> localMessageQueue;

    private final Optional<Long> offerTimeout;
    private final Optional<TimeUnit> offerTimeoutUnit;

    @Inject
    BlockingQueueMessageProducerPlugin(@ServiceConfig final Config serviceConfig,
                                       @Named("localMessageQueue") final BlockingQueue<Message> localMessageQueue) {
        this.localMessageQueue = localMessageQueue;
        this.offerTimeout = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("local.messageProducer.offerTimeout"))
                .map(config -> config.getLong("local.messageProducer.offerTimeout"));
        this.offerTimeoutUnit = Optional.of(serviceConfig)
                .filter(config -> config.hasPath("local.messageProducer.offerTimeoutUnit"))
                .map(config -> config.getEnum(TimeUnit.class, "local.messageProducer.offerTimeoutUnit"));
        checkState(offerTimeout.isPresent() == offerTimeoutUnit.isPresent(),
                "local.queue.offerTimeout and local.queue.offerTimeoutUnit must both have values");
    }

    @Override
    public void sendMessage(final Message message) {
        try {
            if (offerTimeout.isPresent()) {
                localMessageQueue.offer(message, offerTimeout.get(), offerTimeoutUnit.get());
            } else {
                localMessageQueue.offer(message);
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while sending message on local queue", e);
        }
    }
}
