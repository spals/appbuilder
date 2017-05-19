package net.spals.appbuilder.model.core.pojo;

import org.inferred.freebuilder.FreeBuilder;

/**
 * FreeBuilder test class for SerializableMesssageFormatterTest.
 *
 * This is separated so that generated classes can be created.
 *
 * @author tkral
 */
@FreeBuilder
public abstract class BuilderClass {

    abstract String getStringValue();

    static class Builder extends BuilderClass_Builder {  }
}
