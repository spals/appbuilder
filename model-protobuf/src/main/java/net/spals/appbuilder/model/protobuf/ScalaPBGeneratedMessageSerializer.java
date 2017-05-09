package net.spals.appbuilder.model.protobuf;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.auto.value.AutoValue;
import com.trueaccord.scalapb.GeneratedMessage;
import com.trueaccord.scalapb.GeneratedMessageCompanion;

import java.lang.reflect.Method;

import static com.esotericsoftware.kryo.Kryo.NULL;

/**
 * @author tkral
 */
@AutoValue
abstract class ScalaPBGeneratedMessageSerializer extends Serializer<GeneratedMessage> {

    abstract GeneratedMessageCompanion<?> getCompanion();

    static ScalaPBGeneratedMessageSerializer create(final Class type) {
        final GeneratedMessageCompanion<?> companion;
        try {
            final Method companionMethod = type.getMethod("messageCompanion");
            companion = (GeneratedMessageCompanion<?>) companionMethod.invoke(type);
        } catch (Exception e) {
            throw new RuntimeException("Using ScalaPBGeneratedMessageSerializer on non-ScalaPB type: " + type);
        }

        return create(companion);
    }

    static ScalaPBGeneratedMessageSerializer create(final GeneratedMessageCompanion<?> companion) {
        return new AutoValue_ScalaPBGeneratedMessageSerializer(companion);
    }

    @Override
    public void write(final Kryo kryo, final Output output, final GeneratedMessage protobufMessage) {
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
    public GeneratedMessage read(final Kryo kryo, final Input input, final Class<GeneratedMessage> type) {
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
        return (GeneratedMessage) getCompanion().parseFrom(bytes);
    }
}
