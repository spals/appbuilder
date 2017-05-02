package net.spals.appbuilder.message.core.formatter;

import java.io.IOException;

/**
 * @author tkral
 */
public interface MessageFormatter {

    Object deserializePayload(final byte[] serializedPayload) throws IOException;

    byte[] serializePayload(final Object payload) throws IOException;
}
