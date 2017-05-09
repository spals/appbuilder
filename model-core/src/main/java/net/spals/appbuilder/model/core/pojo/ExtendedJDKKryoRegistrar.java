package net.spals.appbuilder.model.core.pojo;

import com.esotericsoftware.kryo.Kryo;
import com.twitter.chill.IKryoRegistrar;
import de.javakaffee.kryoserializers.*;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * An {@link IKryoRegistrar} implementation for
 * the kryo-serializers library's extended JDK serializers.
 *
 * @author tkral
 */
class ExtendedJDKKryoRegistrar implements IKryoRegistrar {

    @Override
    public void apply(final Kryo kryo) {
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
}
