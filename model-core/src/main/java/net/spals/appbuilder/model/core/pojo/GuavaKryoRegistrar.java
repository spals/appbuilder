package net.spals.appbuilder.model.core.pojo;

import com.esotericsoftware.kryo.Kryo;
import com.twitter.chill.IKryoRegistrar;
import de.javakaffee.kryoserializers.guava.*;

/**
 * An {@link IKryoRegistrar} implementation for
 * the kryo-serializers library's guava serializers.
 *
 * @author tkral
 */
class GuavaKryoRegistrar implements IKryoRegistrar {

    @Override
    public void apply(final Kryo kryo) {
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
}
