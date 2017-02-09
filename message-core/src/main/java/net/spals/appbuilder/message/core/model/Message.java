package net.spals.appbuilder.message.core.model;

import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;

/**
 * @author tkral
 */
@FreeBuilder
public interface Message {

    Map<String, Object> getPayload();

    String getTag();

    class Builder extends Message_Builder {  }
}
