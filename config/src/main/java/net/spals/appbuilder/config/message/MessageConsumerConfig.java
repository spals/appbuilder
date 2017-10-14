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
public abstract class MessageConsumerConfig implements TaggedConfig {

    private static final String CHANNEL_KEY = "channel";
    private static final String FORMAT_KEY = "format";
    private static final String GLOBAL_ID_KEY = "globalId";
    private static final String SOURCE_KEY = "source";

    @JsonProperty(TaggedConfig.ACTIVE_KEY)
    public abstract Optional<Boolean> getActive();

    @Override
    @JsonIgnore
    public final boolean isActive() {
        return getActive().orElse(true);
    }

    @JsonProperty(CHANNEL_KEY)
    public abstract String getChannel();

    @JsonProperty(FORMAT_KEY)
    public abstract String getFormat();

    @JsonProperty(GLOBAL_ID_KEY)
    public abstract String getGlobalId();

    @JsonProperty(SOURCE_KEY)
    public abstract String getSource();

    @Override
    public abstract String getTag();

    @Override
    public final String toString() {
        return String.format("%s(%s)[ch->%s,fmt->%s,gId->%s,src->%s]", getTag(),
            isActive() ? "ACTIVE" : "INACTIVE",
            getChannel(), getFormat(), getGlobalId(), getSource());
    }

    public static class Builder extends MessageConsumerConfig_Builder {  }
}
