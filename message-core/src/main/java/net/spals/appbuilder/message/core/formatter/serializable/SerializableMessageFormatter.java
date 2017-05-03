package net.spals.appbuilder.message.core.formatter.serializable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.*;
import com.google.inject.Inject;
import de.javakaffee.kryoserializers.*;
import de.javakaffee.kryoserializers.guava.*;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.message.core.formatter.MessageFormatter;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author tkral
 */
@AutoBindInMap(baseClass = MessageFormatter.class, key = "serializable")
class SerializableMessageFormatter implements MessageFormatter {

    private final Kryo kryo;

    SerializableMessageFormatter() {
        kryo = new KryoReflectionFactorySupport();
        registerExtendedJDKSerializers(kryo);
        registerGuavaSerializers(kryo);
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
        kryo.register(ArrayListMultimap.class, new ArrayListMultimapSerializer());
        kryo.register(HashMultimap.class, new HashMultimapSerializer());
        kryo.register(ImmutableList.class, new ImmutableListSerializer());
        kryo.register(ImmutableMap.class, new ImmutableMapSerializer());
        kryo.register(ImmutableMultimap.class, new ImmutableMultimapSerializer());
        kryo.register(ImmutableSet.class, new ImmutableSetSerializer());
        kryo.register(ImmutableSortedSet.class, new ImmutableSortedSetSerializer());
        kryo.register(LinkedHashMultimap.class, new LinkedHashMultimapSerializer());
        kryo.register(LinkedListMultimap.class, new LinkedListMultimapSerializer());
        kryo.register(TreeMultimap.class, new TreeMultimapSerializer());
        UnmodifiableNavigableSetSerializer.registerSerializers(kryo);
    }

    @Override
    public Object deserializePayload(final byte[] serializedPayload) {
        try (final Input kryoInput = new Input(serializedPayload)) {
            return kryo.readClassAndObject(kryoInput);
        }
    }

    @Override
    public byte[] serializePayload(final Object payload) {
        try (final Output kryoOutput = new Output(32 /*bufferSize*/, -1 /*maxBufferSize*/)) {
            kryo.writeClassAndObject(kryoOutput, payload);
            return kryoOutput.getBuffer();
        }
    }
}
