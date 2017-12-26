package net.spals.appbuilder.mapstore.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindProvider;
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions;
import net.spals.appbuilder.mapstore.core.model.MapStoreIndexName;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.spals.appbuilder.mapstore.core.MapStoreProvider.DelegatingMapStore.checkSingleItemKey;

/**
 * @author tkral
 */
@AutoBindProvider
class MapStoreIndexProvider implements Provider<MapStoreIndex> {

    @Configuration("mapStore.system")
    private volatile String storeSystem;

    private final Map<String, MapStoreIndexPlugin> storeIndexPluginMap;

    @Inject
    MapStoreIndexProvider(final Map<String, MapStoreIndexPlugin> storeIndexPluginMap) {
        this.storeIndexPluginMap = storeIndexPluginMap;
    }

    @Override
    public MapStoreIndex get() {
        final MapStoreIndexPlugin storeIndexPlugin = Optional.ofNullable(storeIndexPluginMap.get(storeSystem))
            .orElseThrow(() -> new ConfigException.BadValue("mapStore.system",
                    "No Map Store Index plugin found for : " + storeSystem));

        return new DelegatingMapStoreIndex(storeIndexPlugin);
    }

    @VisibleForTesting
    static class DelegatingMapStoreIndex implements MapStoreIndex {

        private final MapStoreIndexPlugin pluginDelegate;

        DelegatingMapStoreIndex(final MapStoreIndexPlugin pluginDelegate) {
            this.pluginDelegate = pluginDelegate;
        }

        @Override
        public void close() {
            // Plugins should register their close() method
            // in the pre-destroy lifecycle so we're sure that
            // everything is properly torn down. Therefore, there's
            // nothing to do here.
        }

        @Override
        public boolean createIndex(
            final MapStoreIndexName indexName,
            final MapStoreTableKey indexKey
        ) {
            return pluginDelegate.createIndex(indexName, indexKey);
        }

        @Override
        public boolean dropIndex(final MapStoreIndexName indexName) {
            return pluginDelegate.dropIndex(indexName);
        }

        @Override
        public Optional<Map<String, Object>> getItem(
            final MapStoreIndexName indexName,
            final MapStoreKey key
        ) {
            checkSingleItemKey(key);
            return pluginDelegate.getItem(indexName, key);
        }

        @Override
        public List<Map<String, Object>> getItems(
            final MapStoreIndexName indexName,
            final MapStoreKey key,
            final MapQueryOptions options
        ) {
            return pluginDelegate.getItems(indexName, key, options);
        }
    }
}
