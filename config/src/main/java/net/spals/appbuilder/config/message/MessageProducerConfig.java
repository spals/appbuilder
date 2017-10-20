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
public abstract class MessageProducerConfig implements TaggedConfig {

    private static final String CHANNEL_KEY = "channel";
    private static final String DESTINATION_KEY = "destination";
    private static final String FORMAT_KEY = "format";
    private static final String GLOBAL_ID_KEY = "globalId";

    @JsonProperty(TaggedConfig.ACTIVE_KEY)
    public abstract Optional<Boolean> getActive();

    @Override
    @JsonIgnore
    public final boolean isActive() {
        return getActive().orElse(true);
    }

    @JsonProperty(CHANNEL_KEY)
    public abstract String getChannel();

    @JsonProperty(DESTINATION_KEY)
    public abstract String getDestination();

    @JsonProperty(FORMAT_KEY)
    public abstract String getFormat();

    @JsonProperty(GLOBAL_ID_KEY)
    public abstract String getGlobalId();

    public abstract String getTag();

    @Override
    public final String toString() {
        return String.format("%s(%s)[ch->%s,dest->%s,fmt->%s,gId->%s]", getTag(),
            isActive() ? "ACTIVE" : "INACTIVE",
            getChannel(), getDestination(), getFormat(), getGlobalId());
    }

    public static class Builder extends MessageProducerConfig_Builder {  }
}
