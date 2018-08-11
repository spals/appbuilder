package net.spals.appbuilder.model.core;

import java.io.IOException;
import java.util.Map;

/**
 * Service definition for model object serialization.
 *
 * @author tkral
 */
public interface ModelSerializer {

    /**
     * Deserializes the given byte array into an {@link Object}
     * using the Kryo library.
     *
     * This method is guaranteed to be the exact inverse of the
     * {@link #serialize(Object)} method.
     */
    Object deserialize(final byte[] bytes);

    /**
     * Serializes the given model {@link Object} to a byte array
     * using the Kryo library.
     *
     * This method is guaranteed to be the exact inverse of the
     * {@link #deserialize(byte[])} method.
     */
    byte[] serialize(final Object modelObject);

    /**
     * Deserializes the given json string into an {@link Object}.
     *
     * This method is guaranteed to be the exact inverse of the
     * {@link #jsonSerialize(Object)} method.
     */
    Object jsonDeserialize(final String json) throws IOException;

    /**
     * Serializes the given model {@link Object} to a byte array
     * using the JSON format.
     *
     * This method is guaranteed to be the exact inverse of the
     * {@link #jsonDeserialize(String)} method.
     */
    String jsonSerialize(final Object modelObject) throws IOException;
}
