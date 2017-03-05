package net.spals.appbuilder.message.core.formatter;

import java.io.IOException;
import java.util.Map;

/**
 * @author tkral
 */
public interface MessageFormatter {

    Map<String, Object> deserializePayload(final byte[] serializedPayload) throws IOException;

    byte[] serializePayload(final Map<String, Object> payload) throws IOException;
}
