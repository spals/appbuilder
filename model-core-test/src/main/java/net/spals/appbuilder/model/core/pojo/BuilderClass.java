package net.spals.appbuilder.model.core.pojo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

/**
 * FreeBuilder test class for SerializableMesssageFormatterTest.
 *
 * This is separated so that generated classes can be created.
 *
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = BuilderClass.Builder.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public abstract class BuilderClass {

    abstract String getStringValue();

    static class Builder extends BuilderClass_Builder {  }
}
