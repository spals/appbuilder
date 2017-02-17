package net.spals.appbuilder.mapstore.core;

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

    void deleteItem(String tableName, MapStoreKey key);

    Optional<Map<String, Object>> getItem(String tableName, MapStoreKey key);

    List<Map<String, Object>> getItems(String tableName, MapStoreKey key);

    Map<String, Object> putItem(String tableName, MapStoreKey key, Map<String, Object> payload);

    Map<String, Object> updateItem(String tableName, MapStoreKey key, Map<String, Object> payload);
}
