package net.spals.appbuilder.model.core;

import java.io.IOException;

/**
 * Service definition for model object serializer.
 *
 * @author tkral
 */
public interface ModelSerializer {

    /**
     * Deserializes the given byte array into an {@link Object}.
     *
     * The returned object type is dependent on the particular
     * service implementation. Note that type information is
     * not provided in the signature so it must be inferred
     * in the byte array itself.
     *
     * This method is guaranteed to be the exact inverse of the
     * {@link #serialize(Object)} method.
     */
    Object deserialize(final byte[] serializedModelObject) throws IOException;

    /**
     * Serializes the given model {@link Object} to a byte array.
     *
     * The serialization format is dependent on the particular
     * service implementation. Note that type information must
     * also be included in the returned byte array to support
     * {@link #deserialize(byte[])}.
     *
     * This method is guaranteed to be the exact inverse of the
     * {@link #deserialize(byte[])} method.
     */
    byte[] serialize(final Object modelObject) throws IOException;
}
