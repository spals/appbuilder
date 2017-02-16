package net.spals.appbuilder.message.core.formatter.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.message.core.formatter.MessageFormatter;

import java.io.IOException;
import java.util.Map;

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
    public Map<String, Object> deserializePayload(final byte[] serializedPayload) throws IOException {
        return objectMapper.readValue(serializedPayload, new TypeReference<Map<String, Object>>() {});
    }

    @Override
    public byte[] serializePayload(final Map<String, Object> payload) throws IOException {
        return objectMapper.writeValueAsBytes(payload);
    }
}
