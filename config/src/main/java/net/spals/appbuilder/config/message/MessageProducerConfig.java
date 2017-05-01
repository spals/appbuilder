package net.spals.appbuilder.config.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.spals.appbuilder.config.TaggedConfig;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

/**
 * Common configuration for an event producer.
 *
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = MessageProducerConfig.Builder.class)
public interface MessageProducerConfig extends TaggedConfig {

    String CHANNEL_KEY = "channel";
    String DESTINATION_KEY = "destination";
    String FORMAT_KEY = "format";
    String GLOBAL_ID_KEY = "globalId";

    @JsonProperty(TaggedConfig.ACTIVE_KEY)
    Optional<Boolean> getActive();

    @Override
    @JsonIgnore
    default boolean isActive() {
        return getActive().orElse(true);
    }

    @JsonProperty(MessageProducerConfig.CHANNEL_KEY)
    String getChannel();

    @JsonProperty(MessageProducerConfig.DESTINATION_KEY)
    String getDestination();

    @JsonProperty(MessageProducerConfig.FORMAT_KEY)
    String getFormat();

    @JsonProperty(MessageProducerConfig.GLOBAL_ID_KEY)
    String getGlobalId();

    String getTag();

    class Builder extends MessageProducerConfig_Builder {  }
}
