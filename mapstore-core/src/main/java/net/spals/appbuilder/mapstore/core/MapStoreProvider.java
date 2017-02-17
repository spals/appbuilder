package net.spals.appbuilder.mapstore.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindProvider;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindProvider
class MapStoreProvider implements Provider<MapStore> {

    @Configuration("map.store.system")
    private volatile String storeSystem;

    private final Map<String, MapStorePlugin> storePluginMap;

    @Inject
    MapStoreProvider(final Map<String, MapStorePlugin> storePluginMap) {
        this.storePluginMap = storePluginMap;
    }

    @Override
    public MapStore get() {
        final MapStorePlugin storePlugin = Optional.ofNullable(storePluginMap.get(storeSystem))
            .orElseThrow(() -> new ConfigException.BadValue("map.store.system",
                    "No Map Store plugin found for : " + storeSystem));

        return new DelegatingMapStore(storePlugin);
    }

    @VisibleForTesting
    static class DelegatingMapStore implements MapStore {

        private final MapStorePlugin pluginDelegate;

        DelegatingMapStore(final MapStorePlugin pluginDelegate) {
            this.pluginDelegate = pluginDelegate;
        }

        @Override
        public void deleteItem(final String tableName, final MapStoreKey key) {
            pluginDelegate.deleteItem(tableName, key);
        }

        @Override
        public Optional<Map<String, Object>> getItem(final String tableName, final MapStoreKey key) {
            return pluginDelegate.getItem(tableName, key);
        }

        @Override
        public List<Map<String, Object>> getItems(final String tableName, final MapStoreKey key) {
            return pluginDelegate.getItems(tableName, key);
        }

        @Override
        public Map<String, Object> putItem(final String tableName,
                                           final MapStoreKey key,
                                           final Map<String, Object> payload) {
            return pluginDelegate.putItem(tableName, key, payload);
        }

        @Override
        public Map<String, Object> updateItem(final String tableName,
                                              final MapStoreKey key,
                                              final Map<String, Object> payload) {
            return pluginDelegate.updateItem(tableName, key, payload);
        }
    }
}
