package net.spals.appbuilder.mapstore.core.mapdb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.mapstore.core.MapStorePlugin;
import net.spals.appbuilder.mapstore.core.mapdb.MapDBMapStoreIndexPlugin.MapDBUpdateIndexListener;
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions;
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.Order;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Standard;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.TwoValueHolder;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DB.TreeMapMaker;
import org.mapdb.MapModificationListener;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArrayTuple;
import org.mapdb.serializer.SerializerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.spals.appbuilder.mapstore.core.MapStorePlugin.isNullOrEmptyEntry;
import static net.spals.appbuilder.mapstore.core.mapdb.MapDBIndexMetadata.findIndexMetadata;

/**
 * @author tkral
 */
@AutoBindInMap(baseClass = MapStorePlugin.class, key = "mapDB")
class MapDBMapStorePlugin implements MapStorePlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapDBMapStorePlugin.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DB mapDB;

    @Inject
    MapDBMapStorePlugin(final DB mapDB) {
        this.mapDB = mapDB;
    }

    @Override
    @PreDestroy
    public void close() {
        mapDB.close();
    }

    @Override
    public boolean createTable(
        final String tableName,
        final MapStoreTableKey tableKey
    ) {
        final SerializerArrayTuple tableKeySerializer = createKeySerializer(tableKey.getHashFieldType(),
            tableKey.getRangeFieldType());

        mapDB.treeMap(tableName)
            .keySerializer(tableKeySerializer)
            .valueSerializer(Serializer.BYTE_ARRAY)
            .createOrOpen();
        return true;
    }

    @Override
    public boolean dropTable(final String tableName) {
        final BTreeMap<?, ?> table = mapDB.treeMap(tableName).open();
        table.clear();
        table.close();

        return true;
    }

    @Override
    public void deleteItem(
        final String tableName,
        final MapStoreKey key
    ) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertSimpleKeyToArray(key);

        table.remove(keyArray);
    }

    @Override
    public List<Map<String, Object>> getAllItems(final String tableName) {
        final BTreeMap<?, byte[]> table = mapDB.treeMap(tableName)
            .valueSerializer(Serializer.BYTE_ARRAY)
            .open();
        final Stream<Map<String, Object>> valueStream = table.values().stream().map(valueMapper());
        return valueStream.collect(Collectors.toList());

    }

    @Override
    public Optional<Map<String, Object>> getItem(
        final String tableName,
        final MapStoreKey key
    ) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertSimpleKeyToArray(key);

        final Optional<byte[]> valueArray = Optional.ofNullable(table.get(keyArray));
        return valueArray.map(valueMapper());
    }

    @Override
    public List<Map<String, Object>> getItems(
        final String tableName,
        final MapStoreKey key,
        final MapQueryOptions options
    ) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        Collection<byte[]> valueArrays = Collections.emptyList();
        final MapRangeOperator.Standard op = Standard.fromName(key.getRangeKey().getOperator().toString())
            .orElseThrow(() -> new IllegalArgumentException("MapDB cannot support the operator " +
                key.getRangeKey().getOperator()));

        switch (op) {
            case ALL:
                final Object[] allKeyArray = new Object[]{key.getHashValue()};
                valueArrays = table.prefixSubMap(allKeyArray).values();
                break;
            case BETWEEN:
                final Object[] fromKeyArray = new Object[]{key.getHashValue(), ((TwoValueHolder)key.getRangeKey().getValue()).getValue1()};
                final Object[] toKeyArray = new Object[]{key.getHashValue(), ((TwoValueHolder)key.getRangeKey().getValue()).getValue2()};
                valueArrays = table.subMap(fromKeyArray, true, toKeyArray, true).values();
                break;
            case EQUAL_TO:
                final Object[] equalToKeyArray = convertSimpleKeyToArray(key);
                valueArrays = Optional.ofNullable(table.get(equalToKeyArray))
                    .map(value -> Collections.singletonList(value))
                    .orElseGet(() -> Collections.emptyList());
                break;
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL_TO:
                final Object[] greaterThanKey = convertSimpleKeyToArray(key);
                valueArrays = table.tailMap(greaterThanKey,
                    key.getRangeKey().getOperator() == Standard.GREATER_THAN_OR_EQUAL_TO).values();
                break;
            case LESS_THAN:
            case LESS_THAN_OR_EQUAL_TO:
                final Object[] lessThanKey = convertSimpleKeyToArray(key);
                valueArrays = table.headMap(lessThanKey,
                    key.getRangeKey().getOperator() == Standard.LESS_THAN_OR_EQUAL_TO).values();
                break;
        }

        final Stream<Map<String, Object>> valueStream = valueArrays.stream()
            .map(valueMapper())
            .sorted(valueComparator(key, options.getOrder()));
        return options.getLimit().map(limit -> valueStream.limit(limit).collect(Collectors.toList()))
            .orElseGet(() -> valueStream.collect(Collectors.toList()));
    }

    @Override
    public Map<String, Object> putItem(
        final String tableName,
        final MapStoreKey key,
        final Map<String, Object> payload
    ) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertSimpleKeyToArray(key);
        final Map<String, Object> returnValue = new TreeMap<>(payload);

        returnValue.putIfAbsent(key.getHashField(), key.getHashValue());
        key.getRangeField().ifPresent(rangeField -> returnValue.putIfAbsent(rangeField, key.getRangeKey().getValue()));

        try {
            table.put(keyArray, OBJECT_MAPPER.writeValueAsBytes(returnValue));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    @Override
    public Map<String, Object> updateItem(
        final String tableName,
        final MapStoreKey key,
        final Map<String, Object> payload
    ) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertSimpleKeyToArray(key);
        // As a precondition, the item is guaranteed to be present
        final Map<String, Object> returnValue = new TreeMap<>(getItem(tableName, key).get());

        payload.entrySet().stream().forEach(entry -> {
            if (isNullOrEmptyEntry().test(entry)) {
                returnValue.remove(entry.getKey());
            } else {
                returnValue.put(entry.getKey(), entry.getValue());
            }
        });

        try {
            table.put(keyArray, OBJECT_MAPPER.writeValueAsBytes(returnValue));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    void attachTableIndexes(
        final String tableName,
        final BTreeMap<Object[], byte[]> table,
        final Set<MapDBIndexMetadata> indexMetadatas
    ) {
        if (indexMetadatas.isEmpty()) {
            return;
        }

        // Attach indexes to the table. Unfortunately, the map maker doesn't honor
        // modification listeners added after the table is created so we need to use
        // reflection to force them in.
        final Field listenerField;
        try {
            final MapModificationListener<Object[], byte[]>[] indexListeners =
                indexMetadatas.stream().map(indexMetadata -> new MapDBUpdateIndexListener(mapDB, indexMetadata))
                    .toArray(MapDBUpdateIndexListener[]::new);

            listenerField = BTreeMap.class.getDeclaredField("modificationListeners");
            listenerField.setAccessible(true);
            listenerField.set(table, indexListeners);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            LOGGER.error("Unable to attach index listeners to MapDB table " + tableName, e);
            throw new RuntimeException("Unable to attach index listeners to MapDB table " + tableName, e);
        }
    }

    @VisibleForTesting
    Object[] convertSimpleKeyToArray(final MapStoreKey key) {
        return key.getRangeField().map(rangeField -> new Object[]{key.getHashValue(), key.getRangeKey().getValue()})
            .orElseGet(() -> new Object[]{key.getHashValue()});
    }

    @VisibleForTesting
    static SerializerArrayTuple createKeySerializer(
        final Class<?> hashFieldType,
        final Optional<Class<? extends Comparable>> rangeFieldType
    ) {
        final Serializer hashKeySerializer = SerializerUtils.serializerForClass(hashFieldType);
        final Optional<Serializer> rangeKeySerializer = rangeFieldType
            .map(rangeType -> (Serializer) SerializerUtils.serializerForClass(rangeType));
        return rangeKeySerializer
            .map(rangeKeySer -> new SerializerArrayTuple(hashKeySerializer, rangeKeySer))
            .orElseGet(() -> new SerializerArrayTuple(hashKeySerializer));
    }

    @VisibleForTesting
    BTreeMap<Object[], byte[]> getTable(
        final String tableName,
        final MapStoreKey key
    ) {
        final TreeMapMaker<Object[], byte[]> tableMaker = getTableMaker(tableName, key);
        final BTreeMap<Object[], byte[]> table = tableMaker.open();

        final Set<MapDBIndexMetadata> indexMetadatas = findIndexMetadata(mapDB, tableName);
        attachTableIndexes(tableName, table, indexMetadatas);
        return table;
    }

    @VisibleForTesting
    TreeMapMaker<Object[], byte[]> getTableMaker(
        final String tableName,
        final MapStoreKey key
    ) {
        final SerializerArrayTuple storeKeySerializer = createKeySerializer(key.getHashValue().getClass(),
            key.getRangeField().flatMap(rangeField -> {
                final Optional<Comparable<?>> rangeValue = Optional.ofNullable(key.getRangeKey().getValue());
                return rangeValue.map(rValue -> rValue.getClass());
            }));

        return mapDB.treeMap(tableName)
            .keySerializer(storeKeySerializer)
            .valueSerializer(Serializer.BYTE_ARRAY);
    }

    @VisibleForTesting
    Comparator<Map<String, Object>> valueComparator(
        final MapStoreKey key,
        final Order order
    ) {
        return (m1, m2) -> {
            // If there's no range key than order doesn't matter
            if (!key.getRangeField().isPresent()) {
                return 0;
            }

            // Otherwise, sort by the range field values
            final String rangeField = key.getRangeField().get();
            return order == Order.ASC ?
                ((Comparable) m1.get(rangeField)).compareTo(m2.get(rangeField)) :
                ((Comparable) m2.get(rangeField)).compareTo(m1.get(rangeField));
        };
    }

    @VisibleForTesting
    static Function<byte[], Map<String, Object>> valueMapper() {
        return value -> {
            try {
                return OBJECT_MAPPER.readValue(value, new TypeReference<Map<String, Object>>(){});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
