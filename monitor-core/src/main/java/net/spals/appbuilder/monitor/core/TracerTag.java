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
public abstract class TracerTag implements TaggedConfig {

    private static final String TAG_VALUE_KEY = "tagValue";

    @Override
    @JsonIgnore
    public final boolean isActive() {
        return true;
    }

    @JsonIgnore
    public final String getKey() {
        return getTag();
    }

    @JsonProperty(TAG_VALUE_KEY)
    public abstract Object getValue();

    @Override
    public abstract String getTag();

    @Override
    public final String toString() {
        return getTag() + "->" + getValue();
    }

    public static class Builder extends TracerTag_Builder {  }
}
