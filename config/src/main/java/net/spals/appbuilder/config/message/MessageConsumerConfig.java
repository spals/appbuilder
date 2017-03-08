package net.spals.appbuilder.config.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.spals.appbuilder.config.TaggedConfig;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

/**
 * Common configuration for an asynchronous message consumer.
 *
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = MessageConsumerConfig.Builder.class)
public interface MessageConsumerConfig extends TaggedConfig {

    String CHANNEL_KEY = "channel";
    String FORMAT_KEY = "format";
    String GLOBAL_ID_KEY = "globalId";
    String SOURCE_KEY = "source";

    @JsonProperty(TaggedConfig.ACTIVE_KEY)
    Optional<Boolean> getActive();

    @Override
    @JsonIgnore
    default boolean isActive() {
        return getActive().orElse(true);
    }

    @JsonProperty(MessageConsumerConfig.CHANNEL_KEY)
    String getChannel();

    @JsonProperty(MessageConsumerConfig.FORMAT_KEY)
    String getFormat();

    @JsonProperty(MessageConsumerConfig.GLOBAL_ID_KEY)
    String getGlobalId();

    @JsonProperty(MessageConsumerConfig.SOURCE_KEY)
    String getSource();

    @Override
    String getTag();

    class Builder extends MessageConsumerConfig_Builder {  }
}
