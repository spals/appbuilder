package net.spals.appbuilder.nosqlstore.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindProvider;
import net.spals.appbuilder.nosqlstore.core.model.NoSqlStoreKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindProvider
class NoSqlStoreProvider implements Provider<NoSqlStore> {

    @Configuration("nosql.store.system")
    private volatile String storeSystem;

    private final Map<String, NoSqlStorePlugin> storePluginMap;

    @Inject
    NoSqlStoreProvider(final Map<String, NoSqlStorePlugin> storePluginMap) {
        this.storePluginMap = storePluginMap;
    }

    @Override
    public NoSqlStore get() {
        final NoSqlStorePlugin storePlugin = Optional.ofNullable(storePluginMap.get(storeSystem))
            .orElseThrow(() -> new ConfigException.BadValue("nosql.store.system",
                    "No NoSql Store plugin found for : " + storeSystem));

        return new DelegatingNoSqlStore(storePlugin);
    }

    @VisibleForTesting
    static class DelegatingNoSqlStore implements NoSqlStore {

        private final NoSqlStorePlugin pluginDelegate;

        DelegatingNoSqlStore(final NoSqlStorePlugin pluginDelegate) {
            this.pluginDelegate = pluginDelegate;
        }

        @Override
        public void deleteItem(final String tableName, final NoSqlStoreKey key) {
            pluginDelegate.deleteItem(tableName, key);
        }

        @Override
        public Optional<Map<String, Object>> getItem(final String tableName, final NoSqlStoreKey key) {
            return pluginDelegate.getItem(tableName, key);
        }

        @Override
        public List<Map<String, Object>> getItems(final String tableName, final NoSqlStoreKey key) {
            return pluginDelegate.getItems(tableName, key);
        }

        @Override
        public Map<String, Object> putItem(final String tableName,
                                           final NoSqlStoreKey key,
                                           final Map<String, Object> payload) {
            return pluginDelegate.putItem(tableName, key, payload);
        }

        @Override
        public Map<String, Object> updateItem(final String tableName,
                                              final NoSqlStoreKey key,
                                              final Map<String, Object> payload) {
            return pluginDelegate.updateItem(tableName, key, payload);
        }
    }
}
