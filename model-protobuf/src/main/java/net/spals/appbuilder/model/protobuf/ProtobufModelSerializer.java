package net.spals.appbuilder.model.protobuf;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.util.JsonFormat;
import com.trueaccord.scalapb.GeneratedMessage;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.model.core.ModelSerializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author tkral
 */
@AutoBindInMap(baseClass = ModelSerializer.class, key = "protobuf")
class ProtobufModelSerializer implements ModelSerializer {
    private final Kryo kryo;
    private final AtomicReference<JsonFormat.TypeRegistry> jsonTypeRegistry;

    @Inject
    ProtobufModelSerializer() {
        this.kryo = new Kryo() {
            @Override
            public Serializer getDefaultSerializer(final Class type) {
            // Case: Java
            if (MessageLite.class.isAssignableFrom(type)) {
                return MessageLiteSerializer.create(type);
            }

            // Case: Scala
            return ScalaPBGeneratedMessageSerializer.create(type);
            }
        };

        this.jsonTypeRegistry = new AtomicReference<>(JsonFormat.TypeRegistry.getEmptyTypeRegistry());
    }

    @Override
    public Object deserialize(final byte[] bytes) {
        try (final Input kryoInput = new Input(bytes)) {
            return kryo.readClassAndObject(kryoInput);
        }
    }

    @Override
    public byte[] serialize(final Object modelObject) {
        checkArgument(modelObject instanceof Message || modelObject instanceof GeneratedMessage,
                "Cannot serialize non Protobuf object %s", modelObject.getClass());

        try (final Output kryoOutput = new Output(32 /*bufferSize*/, -1 /*maxBufferSize*/)) {
            kryo.writeClassAndObject(kryoOutput, modelObject);
            return kryoOutput.getBuffer();
        }
    }

    @Override
    public Object jsonDeserialize(final String json) throws IOException {
        // Use the stored TypeRegistry to parse the given json into
        // a generic Any Protobuf
        final Any.Builder anyBuilder = Any.newBuilder();
        JsonFormat.parser().usingTypeRegistry(jsonTypeRegistry.get()).merge(json, anyBuilder);
        final Any anyModelObject = anyBuilder.build();
        // Lookup the type Descriptor using type information delivered to us in the json payload
        final String[] parsedJsonTypeUrl = anyModelObject.getTypeUrl().split("/");
        final String jsonTypeName = parsedJsonTypeUrl[parsedJsonTypeUrl.length - 1];
        final Descriptor jsonTypeDescriptor = jsonTypeRegistry.get().find(jsonTypeName);

        // Using a DynamicMessage wrapper, parse the strongly typed Protobuf
        // message from the Any byte string
        return DynamicMessage.getDefaultInstance(jsonTypeDescriptor)
            .getParserForType()
            .parseFrom(anyModelObject.getValue());
    }

    @Override
    public String jsonSerialize(final Object modelObject) throws IOException {
        // TODO: Handle ScalaPB
        checkArgument(modelObject instanceof Message,
            "Cannot serialize non Protobuf object %s", modelObject.getClass());

        final JsonFormat.TypeRegistry typeRegistry = jsonTypeRegistry.updateAndGet(registry -> {
            // Check if our current TypeRegistry has the type that we're trying to serialize
            final Descriptor descriptor = registry.find(((Message) modelObject).getDescriptorForType().getFullName());
            // If so, we're done
            if (descriptor != null) {
                return registry;
            }
            // If not, create a new TypeRegistry will all previously registered types
            // plus the one new type.
            try {
                final Field typesField = registry.getClass().getDeclaredField("types");
                typesField.setAccessible(true);

                return JsonFormat.TypeRegistry.newBuilder()
                    .add(((Map<String, Descriptor>) typesField.get(registry)).values())
                    .add(((Message) modelObject).getDescriptorForType())
                    .build();
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        });

        final Any anyModelObject = Any.<Message>pack((Message) modelObject);
        return JsonFormat.printer()
            .usingTypeRegistry(typeRegistry)
            .omittingInsignificantWhitespace()
            .print(anyModelObject);
    }
}
