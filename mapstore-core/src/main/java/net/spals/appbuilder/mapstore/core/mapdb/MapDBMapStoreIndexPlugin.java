package net.spals.appbuilder.mapstore.core.mapdb;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.MapStoreIndexPlugin;
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions;
import net.spals.appbuilder.mapstore.core.model.MapStoreIndexName;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.MapModificationListener;
import org.mapdb.Serializer;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static net.spals.appbuilder.mapstore.core.mapdb.MapDBIndexMetadata.findIndexMetadata;
import static net.spals.appbuilder.mapstore.core.mapdb.MapDBIndexMetadata.indexMetadata;
import static net.spals.appbuilder.mapstore.core.mapdb.MapDBMapStorePlugin.createKeySerializer;

/**
 * @author tkral
 */
@AutoBindInMap(baseClass = MapStoreIndexPlugin.class, key = "mapDB")
class MapDBMapStoreIndexPlugin implements MapStoreIndexPlugin {

    private final DB mapDB;
    private final MapStore mapStore;

    @Inject
    MapDBMapStoreIndexPlugin(
        final DB mapDB,
        final MapStore mapStore
    ) {
        this.mapDB = mapDB;
        this.mapStore = mapStore;
    }

    @Override
    @PreDestroy
    public void close() {  }

    @Override
    public boolean createIndex(
        final MapStoreIndexName indexName,
        final MapStoreTableKey indexKey
    ) {
        // Create a new index table using the map store
        final MapDBIndexMetadata indexMetadata = indexMetadata(indexName, indexKey);
        final boolean createdIndex = mapStore.createTable(indexMetadata.toString(), indexKey);

        if (createdIndex) {
            // Copy all table records to the new index
            final Function<byte[], Object[]> indexKeyFunction = indexKeyFunction(indexMetadata);
            final BTreeMap<?, byte[]> table = mapDB.treeMap(indexName.getTableName())
                .valueSerializer(Serializer.BYTE_ARRAY)
                .open();
            final BTreeMap<Object[], byte[]> index = mapDB.treeMap(indexMetadata.toString())
                .keySerializer(createKeySerializer(indexKey.getHashFieldType(), indexKey.getRangeFieldType()))
                .valueSerializer(Serializer.BYTE_ARRAY)
                .open();

            table.getValues().forEach((byte[] value) -> {
                final Object[] newKey = indexKeyFunction.apply(value);
                index.put(newKey, value);
            });
        }

        return createdIndex;
    }

    @Override
    public boolean dropIndex(final MapStoreIndexName indexName) {
        return mapStore.dropTable(findIndexMetadata(mapDB, indexName).toString());
    }

    @Override
    public Optional<Map<String, Object>> getItem(
        final MapStoreIndexName indexName,
        final MapStoreKey key
    ) {
        return mapStore.getItem(findIndexMetadata(mapDB, indexName).toString(), key);
    }

    @Override
    public List<Map<String, Object>> getItems(
        final MapStoreIndexName indexName,
        final MapStoreKey key,
        final MapQueryOptions options
    ) {
        return mapStore.getItems(findIndexMetadata(mapDB, indexName).toString(), key, options);
    }

    static Function<byte[], Object[]> indexKeyFunction(final MapDBIndexMetadata indexMetadata) {
        return (byte[] value) -> {
            final Map<String, Object> mapValue = MapDBMapStorePlugin.valueMapper().apply(value);
            final ImmutableList.Builder<Object> keyValuesBuilder = ImmutableList.builder()
                .add(mapValue.get(indexMetadata.getIndexKey().getHashField()));
            indexMetadata.getIndexKey().getRangeField().ifPresent(rangeField ->
                keyValuesBuilder.add(rangeField));

            final List<Object> keyValues = keyValuesBuilder.build();
            return keyValues.toArray(new Object[keyValues.size()]);
        };
    }

    static class MapDBUpdateIndexListener implements MapModificationListener<Object[], byte[]> {

        private final Function<byte[], Object[]> keyFunction;
        private final BTreeMap<Object[], byte[]> index;

        MapDBUpdateIndexListener(
            final DB mapDB,
            final MapDBIndexMetadata indexMetadata
        ) {
            this.keyFunction = MapDBMapStoreIndexPlugin.indexKeyFunction(indexMetadata);

            this.index = mapDB.treeMap(indexMetadata.toString())
                .keySerializer(createKeySerializer(
                    indexMetadata.getIndexKey().getHashFieldType(),
                    indexMetadata.getIndexKey().getRangeFieldType()
                ))
                .valueSerializer(Serializer.BYTE_ARRAY)
                .open();
        }

        @Override
        public void modify(
            final @NotNull Object[] key,
            final @Nullable byte[] oldValue,
            final @Nullable byte[] newValue,
            final boolean triggered
        ) {
            if (newValue == null) {
                final Object[] oldKey = keyFunction.apply(oldValue);
                index.remove(oldKey);
            } else {
                final Object[] newKey = keyFunction.apply(newValue);
                index.put(newKey, newValue);
            }
        }
    }
}
