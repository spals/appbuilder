package net.spals.appbuilder.store.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindProvider;
import net.spals.appbuilder.store.core.model.StoreKey;

import java.util.Map;
import java.util.Optional;

/**
 * @author tkral
 */
@AutoBindProvider
class StoreProvider implements Provider<Store> {

    @Configuration("store.system")
    private volatile String storeSystem;

    private final Map<String, StorePlugin> storePluginMap;

    @Inject
    StoreProvider(final Map<String, StorePlugin> storePluginMap) {
        this.storePluginMap = storePluginMap;
    }

    @Override
    public Store get() {
        final StorePlugin storePlugin = Optional.ofNullable(storePluginMap.get(storeSystem))
            .orElseThrow(() -> new ConfigException.BadValue("store.system",
                    "No store plugin found for : " + storeSystem));

        return new DelegatingStore(storePlugin);
    }

    @VisibleForTesting
    static class DelegatingStore implements Store {

        private final StorePlugin pluginDelegate;

        DelegatingStore(final StorePlugin pluginDelegate) {
            this.pluginDelegate = pluginDelegate;
        }

        @Override
        public void deleteItem(final String tableName, final StoreKey key) {
            pluginDelegate.deleteItem(tableName, key);
        }

        @Override
        public Map<String, Object> getItem(final String tableName, final StoreKey key) {
            return pluginDelegate.getItem(tableName, key);
        }

        @Override
        public Map<String, Object> putItem(final String tableName,
                                           final StoreKey key,
                                           final Map<String, Object> payload) {
            return pluginDelegate.putItem(tableName, key, payload);
        }

        @Override
        public Map<String, Object> updateItem(final String tableName,
                                              final StoreKey key,
                                              final Map<String, Object> payload) {
            return pluginDelegate.updateItem(tableName, key, payload);
        }
    }
}
