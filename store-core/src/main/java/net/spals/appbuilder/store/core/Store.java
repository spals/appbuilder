package net.spals.appbuilder.store.core;

import net.spals.appbuilder.store.core.model.StoreKey;

import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
public interface Store {

    void deleteItem(String tableName, StoreKey key);

    Optional<Map<String, Object>> getItem(String tableName, StoreKey key);

    Map<String, Object> putItem(String tableName, StoreKey key, Map<String, Object> payload);

    Map<String, Object> updateItem(String tableName, StoreKey key, Map<String, Object> payload);
}
