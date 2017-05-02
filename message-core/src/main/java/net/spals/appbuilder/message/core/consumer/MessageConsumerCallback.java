package net.spals.appbuilder.message.core.consumer;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.config.message.MessageConsumerConfig;

import java.util.Map;

/**
 * @author tkral
 */
public interface MessageConsumerCallback<P> {

    String getTag();

    Class<P> getPayloadType();

    void processMessage(MessageConsumerConfig consumerConfig, P payload);

    @AutoValue
    abstract class Key {

        public static Key create(final MessageConsumerCallback<?> consumerCallback) {
            return create(consumerCallback.getTag(), consumerCallback.getPayloadType());
        }

        public static Key create(final String tag, final Class<?> payloadType) {
            return new AutoValue_MessageConsumerCallback_Key(tag, payloadType);
        }

        public abstract String getTag();
        public abstract Class<?> getPayloadType();
    }
}
