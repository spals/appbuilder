package net.spals.appbuilder.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

/**
 * Common configuration for an event producer.
 *
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = ProducerConfig.Builder.class)
public interface ProducerConfig {

    String ACTIVE_KEY = "active";
    String CHANNEL_KEY = "channel";
    String DESTINATION_KEY = "destination";
    String FORMAT_KEY = "format";
    String GLOBAL_ID_KEY = "globalId";

    @JsonProperty(ProducerConfig.ACTIVE_KEY)
    Optional<Boolean> getActive();

    @JsonIgnore
    default boolean isActive() {
        return getActive().orElse(true);
    }

    @JsonProperty(ProducerConfig.CHANNEL_KEY)
    String getChannel();

    @JsonProperty(ProducerConfig.DESTINATION_KEY)
    String getDestination();

    @JsonProperty(ProducerConfig.FORMAT_KEY)
    String getFormat();

    @JsonProperty(ProducerConfig.GLOBAL_ID_KEY)
    String getGlobalId();

    String getTag();

    class Builder extends ProducerConfig_Builder {  }
}
