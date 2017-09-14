package net.spals.appbuilder.monitor.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.opentracing.Tracer;
import org.inferred.freebuilder.FreeBuilder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
public interface TracerTag {

    @JsonProperty("key")
    String getKey();

    @JsonProperty("value")
    Object getValue();

    class Builder extends TracerTag_Builder {  }
}
