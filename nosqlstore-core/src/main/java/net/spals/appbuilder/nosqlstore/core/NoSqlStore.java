package net.spals.appbuilder.nosqlstore.core;

import net.spals.appbuilder.nosqlstore.core.model.NoSqlStoreKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
public interface NoSqlStore {

    void deleteItem(String tableName, NoSqlStoreKey key);

    Optional<Map<String, Object>> getItem(String tableName, NoSqlStoreKey key);

    List<Map<String, Object>> getItems(String tableName, NoSqlStoreKey key);

    Map<String, Object> putItem(String tableName, NoSqlStoreKey key, Map<String, Object> payload);

    Map<String, Object> updateItem(String tableName, NoSqlStoreKey key, Map<String, Object> payload);
}
