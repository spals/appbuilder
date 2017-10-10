package net.spals.appbuilder.mapstore.core;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.netflix.governator.annotations.Configuration;
import com.typesafe.config.ConfigException;
import net.spals.appbuilder.annotations.service.AutoBindProvider;
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions;
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.Order;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Standard;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.SyntacticSugar;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static net.spals.appbuilder.mapstore.core.MapStorePlugin.isNullOrEmptyEntry;

/**
 * @author tkral
 */
@AutoBindProvider
class MapStoreProvider implements Provider<MapStore> {

    @Configuration("mapStore.system")
    private volatile String storeSystem;

    private final Map<String, MapStorePlugin> storePluginMap;

    @Inject
    MapStoreProvider(final Map<String, MapStorePlugin> storePluginMap) {
        this.storePluginMap = storePluginMap;
    }

    @Override
    public MapStore get() {
        final MapStorePlugin storePlugin = Optional.ofNullable(storePluginMap.get(storeSystem))
            .orElseThrow(() -> new ConfigException.BadValue("mapStore.system",
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
        public boolean createTable(final String tableName,
                                   final MapStoreTableKey tableKey) {
            return pluginDelegate.createTable(tableName, tableKey);
        }

        @Override
        public boolean dropTable(final String tableName) {
            return pluginDelegate.dropTable(tableName);
        }

        @Override
        public void deleteItem(final String tableName,
                               final MapStoreKey key) {
            checkSingleItemKey(key);
            pluginDelegate.deleteItem(tableName, key);
        }

        @Override
        public List<Map<String, Object>> getAllItems(final String tableName) {
            return pluginDelegate.getAllItems(tableName);
        }

        @Override
        public Optional<Map<String, Object>> getItem(final String tableName,
                                                     final MapStoreKey key) {
            final Optional<SyntacticSugar> sugarOp = SyntacticSugar.fromName(key.getRangeKey().getOperator().toString());
            if (sugarOp.isPresent()) {
                switch (sugarOp.get()) {
                    case MAX: return getMaxItem(tableName, key);
                    case MIN: return getMinItem(tableName, key);
                    default:
                        throw new IllegalArgumentException("MapStore.getItem does not support the syntactic sugar operator: "+ sugarOp.get().name());
                }
            }

            checkSingleItemKey(key);
            return pluginDelegate.getItem(tableName, key);
        }

        @Override
        public List<Map<String, Object>> getItems(final String tableName,
                                                  final MapStoreKey key,
                                                  final MapQueryOptions options) {
            final Optional<SyntacticSugar> sugarOp = SyntacticSugar.fromName(key.getRangeKey().getOperator().toString());
            if (sugarOp.isPresent()) {
                switch (sugarOp.get()) {
                    default:
                        throw new IllegalArgumentException("MapStore.getItems does not support the syntactic sugar operator: "+ sugarOp.get().name());
                }
            }

            return pluginDelegate.getItems(tableName, key, options);
        }

        @Override
        public Map<String, Object> putItem(final String tableName,
                                           final MapStoreKey key,
                                           final Map<String, Object> payload) {
            checkWriteItem(key, payload);
            checkPutItem(payload);
            return pluginDelegate.putItem(tableName, key, payload);
        }

        @Override
        public Map<String, Object> updateItem(final String tableName,
                                              final MapStoreKey key,
                                              final Map<String, Object> payload) {
            checkWriteItem(key, payload);

            final Optional<Map<String, Object>> item = getItem(tableName, key);
            // If no item is present at the given key, then updateItem takes on putItem semantics
            if (!item.isPresent()) {
                return putItem(tableName, key, payload);
            }

            return pluginDelegate.updateItem(tableName, key, payload);
        }

        @VisibleForTesting
        void checkKeyField(final String keyField, final Object keyValue, final Map<String, Object> payload) {
            checkArgument(!payload.containsKey(keyField) || keyValue.equals(payload.get(keyField)),
                    "Mismatched key value (%s) and payload field value (%s)",
                    keyValue, payload.get(keyField));
        }

        @VisibleForTesting
        void checkPutItem(final Map<String, Object> payload) {
            // Null or empty values have special semantics in updateItem so we'll disallow them here.
            final Set<String> nullValueKeys = payload.entrySet().stream()
                    .filter(isNullOrEmptyEntry())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            checkArgument(nullValueKeys.isEmpty(), "The following keys have null or empty values: %s", nullValueKeys);
        }

        @VisibleForTesting
        void checkSingleItemKey(final MapStoreKey key) {
            if (key.getRangeField().isPresent()) {
                checkArgument(key.getRangeKey().getOperator() == Standard.EQUAL_TO,
                        "Illegal range operator found (%s). You must use EQUAL_TO with value", key.getRangeKey().getOperator());
            } else {
                checkArgument(key.getRangeKey().getOperator() == Standard.NONE,
                        "Illegal range operator found (%s). You must use NONE without value", key.getRangeKey().getOperator());
            }
        }

        @VisibleForTesting
        void checkWriteItem(final MapStoreKey key, final Map<String, Object> payload) {
            checkArgument(!payload.isEmpty(), "Cannot write item with empty payload");
            // Ensure that the hash value matches what's in the payload (or is absent)
            checkKeyField(key.getHashField(), key.getHashValue(), payload);
            // Ensure that the range value matches what's in the payload (or is absent)
            key.getRangeField().ifPresent(rangeField -> checkKeyField(rangeField, key.getRangeKey().getValue(), payload));
            // Write access only one item at a time
            checkSingleItemKey(key);
        }

        // Run max syntactic sugar operation
        Optional<Map<String, Object>> getMaxItem(final String tableName, final MapStoreKey key) {
            checkArgument(key.getRangeKey().getOperator() == SyntacticSugar.MAX);

            // The max operator is equivalent to grabbing all range keys, sorting
            // them in descending order, and grabbing the first one.
            final MapStoreKey maxKey = new MapStoreKey.Builder()
                    .setHash(key.getHashField(), key.getHashValue())
                    .setRange(key.getRangeField().get(), ZeroValueMapRangeKey.all())
                    .build();
            final List<Map<String, Object>> maxItems = getItems(tableName, maxKey,
                    new MapQueryOptions.Builder().setOrder(Order.DESC).setLimit(1).build());
            return Optional.ofNullable(Iterables.getOnlyElement(maxItems, null));
        }

        // Run min syntactic sugar operation
        Optional<Map<String, Object>> getMinItem(final String tableName, final MapStoreKey key) {
            checkArgument(key.getRangeKey().getOperator() == SyntacticSugar.MIN);

            // The min operator is equivalent to grabbing all range keys, sorting
            // them in ascending order, and grabbing the first one.
            final MapStoreKey maxKey = new MapStoreKey.Builder()
                    .setHash(key.getHashField(), key.getHashValue())
                    .setRange(key.getRangeField().get(), ZeroValueMapRangeKey.all())
                    .build();
            final List<Map<String, Object>> minItems = getItems(tableName, maxKey,
                    new MapQueryOptions.Builder().setOrder(Order.ASC).setLimit(1).build());
            return Optional.ofNullable(Iterables.getOnlyElement(minItems, null));
        }
    }
}
