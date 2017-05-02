package net.spals.appbuilder.message.core.formatter.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.message.core.formatter.MessageFormatter;

import java.io.IOException;

/**
 * Default {@link MessageFormatter} which formats
 * messages to and from JSON.
 *
 * @author tkral
 */
@AutoBindInMap(baseClass = MessageFormatter.class, key = "json")
class JsonMessageFormatter implements MessageFormatter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <P> P deserializePayload(final byte[] serializedPayload) throws IOException {
        return objectMapper.readValue(serializedPayload, new TypeReference<P>() {});
    }

    @Override
    public byte[] serializePayload(final Object payload) throws IOException {
        return objectMapper.writeValueAsBytes(payload);
    }
}
