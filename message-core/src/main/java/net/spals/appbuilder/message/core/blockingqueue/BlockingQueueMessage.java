package net.spals.appbuilder.message.core.blockingqueue;

import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;

/**
 * @author tkral
 */
@FreeBuilder
public interface BlockingQueueMessage {

    byte[] getSerializedPayload();

    String getTag();

    class Builder extends BlockingQueueMessage_Builder {  }
}
