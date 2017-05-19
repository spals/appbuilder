package net.spals.appbuilder.message.core;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import net.spals.appbuilder.config.message.MessageConsumerConfig;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

    // Utilities for working with Sets of MessageConsumerCallbacks

    static Map<Class<?>, MessageConsumerCallback<?>> loadCallbacksForTag(final String tag,
                                                                         final Set<MessageConsumerCallback<?>> callbackSet) {
        Preconditions.checkNotNull(tag, "Cannot load consumer callback for null tag");
        return callbackSet.stream().filter(callback -> tag.equals(callback.getTag()))
                .collect(Collectors.toMap(MessageConsumerCallback::getPayloadType, Function.identity()));
    }

    static Map<String, MessageConsumerCallback<?>> loadCallbacksForType(final Class<?> payloadType,
                                                                        final Set<MessageConsumerCallback<?>> callbackSet) {
        Preconditions.checkNotNull(payloadType, "Cannot load consumer callback for null payload type");
        return callbackSet.stream().filter(callback -> payloadType.equals(callback.getPayloadType()))
                .collect(Collectors.toMap(MessageConsumerCallback::getTag, Function.identity()));
    }

    static Collector<MessageConsumerCallback<?>, ?, Map<Key, MessageConsumerCallback<?>>> mapCollector() {
        final Function<MessageConsumerCallback<?>, Key> keyMapper = consumerCallback -> Key.create(consumerCallback);
        return Collectors.toMap(keyMapper, Function.identity());
    }

    static String unregisteredCallbackMessage(final String tag, final Class<?> payloadType) {
        return new StringBuilder("Received payload type ")
            .append(payloadType)
            .append(" for consumer ")
            .append(tag)
            .append(" , but no callback is registered")
            .toString();
    }
}
