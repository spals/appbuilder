package net.spals.appbuilder.mapstore.core;

import net.spals.appbuilder.mapstore.core.model.MapQueryOptions;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A NoSQL storage service which holds
 * data in maps.
 *
 * @author tkral
 */
public interface MapStore {

    /**
     * Delete an item from the given table
     * with the give key.
     *
     * This should be idempotent if the item
     * does not exist.
     */
    void deleteItem(String tableName, MapStoreKey key);

    /**
     * Retrieves all items from the given table
     * with all keys.
     */
    List<Map<String, Object>> getAllItems(String tableName);

    /**
     * Retrieves an item from the given table
     * with the given key.
     *
     * Returns {@link Optional#empty()} if no
     * item exists with the given key.
     */
    Optional<Map<String, Object>> getItem(String tableName, MapStoreKey key);

    /**
     * Queries all items from the given table
     * which match the given {@link MapStoreKey}
     * range key operator.
     */
    List<Map<String, Object>> getItems(String tableName, MapStoreKey key, MapQueryOptions options);

    /**
     * Adds an item to the given table
     * under the given key.
     */
    Map<String, Object> putItem(String tableName, MapStoreKey key, Map<String, Object> payload);

    /**
     * Updates an item in the given table
     * under the given key.
     *
     * If no item exists at the given key,
     * this will fallback to {@link #putItem(String, MapStoreKey, Map)}
     * semantics.
     */
    Map<String, Object> updateItem(String tableName, MapStoreKey key, Map<String, Object> payload);
}
