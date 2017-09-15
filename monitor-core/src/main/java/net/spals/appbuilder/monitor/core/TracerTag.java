package net.spals.appbuilder.monitor.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.opentracing.Tracer;
import net.spals.appbuilder.config.TaggedConfig;
import org.inferred.freebuilder.FreeBuilder;

/**
 * A bean which represents a tag
 * to be attched to a {@link Tracer} instance.
 *
 * We do this so that we can deserialize
 * multiple tags out of the service configuration.
 *
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = TracerTag.Builder.class)
public interface TracerTag extends TaggedConfig {

    String TAG_VALUE_KEY = "tagValue";

    @Override
    @JsonIgnore
    default boolean isActive() {
        return true;
    }

    @JsonIgnore
    default String getKey() {
        return getTag();
    }

    @JsonProperty(TAG_VALUE_KEY)
    Object getValue();

    @Override
    String getTag();

    class Builder extends TracerTag_Builder {  }
}
