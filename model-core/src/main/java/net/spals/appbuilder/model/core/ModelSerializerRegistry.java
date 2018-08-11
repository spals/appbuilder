package net.spals.appbuilder.model.core;

import java.util.Set;

/**
 * A service definition of a registry of
 * all available {@link ModelSerializer}s.
 *
 * @author tkral
 */
public interface ModelSerializerRegistry {

    /**
     * Return the keys of all {@link ModelSerializer}s
     * registered in this registry.
     */
    Set<String> getAvailableModelSerializers();

    /**
     * Find the {@link ModelSerializer} instance
     * registered with this registry using the given key.
     */
    ModelSerializer getModelSerializer(final String key);
}
