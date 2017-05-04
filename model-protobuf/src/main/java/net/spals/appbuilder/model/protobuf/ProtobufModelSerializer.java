package net.spals.appbuilder.model.protobuf;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.protobuf.MessageLite;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.model.core.ModelSerializer;

/**
 * @author tkral
 */
@AutoBindInMap(baseClass = ModelSerializer.class, key = "protobuf")
class ProtobufModelSerializer implements ModelSerializer {

    private final MessageLiteSerializer defaultSerializer;
    private final Kryo kryo;

    @Inject
    ProtobufModelSerializer() {
        this(CacheBuilder.newBuilder());
    }

    @VisibleForTesting
    ProtobufModelSerializer(final CacheBuilder cacheBuilder) {
        this.defaultSerializer = new MessageLiteSerializer(cacheBuilder);
        this.kryo = new Kryo() {
            @Override
            public Serializer getDefaultSerializer(final Class type) {
                return defaultSerializer;
            }
        };
    }

    @VisibleForTesting
    MessageLiteSerializer getDefaultSerializer() {
        return defaultSerializer;
    }

    @Override
    public Object deserialize(final byte[] serializedModelObject) {
        try (final Input kryoInput = new Input(serializedModelObject)) {
            return kryo.readClassAndObject(kryoInput);
        }
    }

    @Override
    public byte[] serialize(final Object modelObject) {
        Preconditions.checkArgument(modelObject instanceof MessageLite,
                "Cannot serialize non Protobuf object %s", modelObject.getClass());

        try (final Output kryoOutput = new Output(32 /*bufferSize*/, -1 /*maxBufferSize*/)) {
            kryo.writeClassAndObject(kryoOutput, modelObject);
            return kryoOutput.getBuffer();
        }
    }
}
