package net.spals.appbuilder.model.core.pojo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.*;
import de.javakaffee.kryoserializers.*;
import de.javakaffee.kryoserializers.guava.*;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.model.core.ModelSerializer;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * A {@link ModelSerializer} implementation based
 * on Plain Old Java Objects.
 *
 * This will also handle Plain Old Scala Objects.
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
    private final Kryo kryo;

    PojoModelSerializer() {
        kryo = new KryoReflectionFactorySupport();
        registerGuavaSerializers(kryo);
        registerExtendedJDKSerializers(kryo);
    }

    private void registerExtendedJDKSerializers(final Kryo kryo) {
        kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
        kryo.register(BitSet.class, new BitSetSerializer());
        kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
        kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
        kryo.register(Collections.singletonMap("","").getClass(), new CollectionsSingletonMapSerializer());
        kryo.register(EnumMap.class, new EnumMapSerializer());
        kryo.register(EnumSet.class, new EnumSetSerializer());
        kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
        kryo.register(Pattern.class, new RegexSerializer());
        kryo.register(URI.class, new URISerializer());
        kryo.register(UUID.class, new UUIDSerializer());
        SynchronizedCollectionsSerializer.registerSerializers(kryo);
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
    }

    private void registerGuavaSerializers(final Kryo kryo) {
        ArrayListMultimapSerializer.registerSerializers(kryo);
        HashMultimapSerializer.registerSerializers(kryo);
        ImmutableListSerializer.registerSerializers(kryo);
        ImmutableMapSerializer.registerSerializers(kryo);
        ImmutableMultimapSerializer.registerSerializers(kryo);
        ImmutableSetSerializer.registerSerializers(kryo);
        ImmutableSortedSetSerializer.registerSerializers(kryo);
        LinkedHashMultimapSerializer.registerSerializers(kryo);
        LinkedListMultimapSerializer.registerSerializers(kryo);
        TreeMultimapSerializer.registerSerializers(kryo);
        UnmodifiableNavigableSetSerializer.registerSerializers(kryo);
    }

    @Override
    public Object deserialize(final byte[] serializedModelObject) {
        try (final Input kryoInput = new Input(serializedModelObject)) {
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
}
