package net.spals.appbuilder.model.protobuf;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import java.lang.reflect.Method;

import static com.esotericsoftware.kryo.Kryo.NULL;

/**
 * A Kryo {@link Serializer} which accepts a generic
 * Protobuf {@link MessageLite}.
 *
 * This is used to power the {@link ProtobufModelSerializer} service.
 *
 * NOTE: Most of this class is shamelessly ripped from the
 * kryo-serializers project.
 *
 * @author tkral
 */
class MessageLiteSerializer extends Serializer<MessageLite> {

    private final LoadingCache<Class<MessageLite>, Parser<? extends MessageLite>> parserCache;

    MessageLiteSerializer(final CacheBuilder parserCacheBuilder) {
        this.parserCache = parserCacheBuilder.build(
                new CacheLoader<Class<MessageLite>, Parser<? extends MessageLite>>() {
                    @Override
                    public Parser<? extends MessageLite> load(final Class<MessageLite> type) throws Exception {
                        final Method parserMethod = type.getMethod("parser");
                        return (Parser<? extends MessageLite>)parserMethod.invoke(type);
                    }
                });
    }

    @VisibleForTesting
    LoadingCache<Class<MessageLite>, Parser<? extends MessageLite>> getParserCache() {
        return parserCache;
    }

    @Override
    public void write(final Kryo kryo, final Output output, final MessageLite protobufMessage) {
        // If our protobuf is null
        if (protobufMessage == null) {
            // Write our special null value
            output.writeByte(NULL);
            output.flush();

            // and we're done
            return;
        }

        // Otherwise serialize protobuf to a byteArray
        byte[] bytes = protobufMessage.toByteArray();

        // Write the length of our byte array
        output.writeInt(bytes.length + 1, true);

        // Write the byte array out
        output.writeBytes(bytes);
        output.flush();
    }

    @Override
    public MessageLite read(final Kryo kryo, final Input input, final Class<MessageLite> type) {
        // Read the length of our byte array
        int length = input.readInt(true);

        // If the length is equal to our special null value
        if (length == NULL) {
            // Just return null
            return null;
        }
        // Otherwise read the byte array length
        byte[] bytes = input.readBytes(length - 1);

        // Deserialize protobuf
        final Parser<? extends MessageLite> parser = parserCache.getUnchecked(type);
        try {
            return parser.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("Unable to deserialize protobuf "+e.getMessage(), e);
        }
    }
}
