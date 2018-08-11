package net.spals.appbuilder.model.core.pojo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.scala.DefaultScalaModule;
import com.twitter.chill.AllScalaRegistrar;
import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.model.core.ModelSerializer;

import java.io.IOException;
import java.util.Map;

import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

/**
 * A {@link ModelSerializer} implementation based
 * on Plain Old Java Objects.
 *
 * IMPORTANT NOTE: Kryo serializer registration order
 * is important (see {@link Kryo#register(Class, Serializer)}).
 * This means that the same version of {@link PojoModelSerializer}
 * must be used for both serialization and deserialization.
 * This is particularly relevant for asynchronous APIs delivered
 * over message queues.
 *
 * @author tkral
 */
@AutoBindInMap(baseClass = ModelSerializer.class, key = "pojo")
class PojoModelSerializer implements ModelSerializer {

    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<Map<String, Object>>() {};

    private final Kryo kryo;
    private final ObjectMapper mapper;

    PojoModelSerializer() {
        kryo = new KryoReflectionFactorySupport();
        new ExtendedJDKKryoRegistrar().apply(kryo);
        new GuavaKryoRegistrar().apply(kryo);
        new AllScalaRegistrar().apply(kryo);

        mapper = new ObjectMapper();
        mapper.disable(FAIL_ON_EMPTY_BEANS); // Don't blow up on empty beans (useful for testing)
        mapper.enableDefaultTyping(NON_FINAL, JsonTypeInfo.As.PROPERTY); // Ensure that type information is added to the JSON string
        mapper.registerModule(new DefaultScalaModule());
        mapper.registerModule(new GuavaModule());
        mapper.registerModule(new Jdk8Module());
    }

    @Override
    public Object deserialize(final byte[] bytes) {
        try (final Input kryoInput = new Input(bytes)) {
            return kryo.readClassAndObject(kryoInput);
        }
    }

    @Override
    public byte[] serialize(final Object modelObject) {
        try (final Output kryoOutput = new Output(32 /*bufferSize*/, -1 /*maxBufferSize*/)) {
            kryo.writeClassAndObject(kryoOutput, modelObject);
            return kryoOutput.getBuffer();
        }
    }

    @Override
    public Object jsonDeserialize(final String json) throws IOException {
        return mapper.readValue(json, Object.class);
    }

    @Override
    public String jsonSerialize(final Object modelObject) throws IOException {
        return mapper.writeValueAsString(modelObject);
    }
}
