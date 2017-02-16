package net.spals.appbuilder.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

/**
 * Common configuration for an asynchronous message consumer.
 *
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = ConsumerConfig.Builder.class)
public interface ConsumerConfig {

    String ACTIVE_KEY = "active";
    String CHANNEL_KEY = "channel";
    String GLOBAL_ID_KEY = "globalId";
    String FORMAT_KEY = "format";
    String SOURCE_KEY = "source";

    @JsonProperty(ConsumerConfig.ACTIVE_KEY)
    Optional<Boolean> getActive();

    @JsonIgnore
    default boolean isActive() {
        return getActive().orElse(true);
    }

    @JsonProperty(ConsumerConfig.CHANNEL_KEY)
    String getChannel();

    @JsonProperty(ConsumerConfig.FORMAT_KEY)
    String getFormat();

    @JsonProperty(ConsumerConfig.GLOBAL_ID_KEY)
    String getGlobalId();

    @JsonProperty(ConsumerConfig.SOURCE_KEY)
    String getSource();

    String getTag();

    class Builder extends ConsumerConfig_Builder {  }
}
