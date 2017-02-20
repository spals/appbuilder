package net.spals.appbuilder.mapstore.core.mapdb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.mapstore.core.MapStorePlugin;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Standard;
import net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.TwoValueHolder;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArrayTuple;
import org.mapdb.serializer.SerializerUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author tkral
 */
@AutoBindInMap(baseClass = MapStorePlugin.class, key = "mapDB")
class MapDBMapStorePlugin implements MapStorePlugin {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DB mapDB;

    MapDBMapStorePlugin(final DB mapDB) {
        this.mapDB = mapDB;
    }

    @Override
    public void deleteItem(final String tableName,
                           final MapStoreKey key) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertSimpleKeyToArray(key);

        table.remove(keyArray);
    }

    @Override
    public Optional<Map<String, Object>> getItem(final String tableName,
                                                 final MapStoreKey key) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertSimpleKeyToArray(key);

        final Optional<byte[]> valueArray = Optional.ofNullable(table.get(keyArray));
        return valueArray.map(valueMapper());
    }

    @Override
    public List<Map<String, Object>> getItems(final String tableName,
                                              final MapStoreKey key) {
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

        return valueArrays.stream()
                .map(valueMapper())
                .sorted(valueComparator(key))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> putItem(final String tableName,
                                       final MapStoreKey key,
                                       final Map<String, Object> payload) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertSimpleKeyToArray(key);
        final Map<String, Object> returnValue = new TreeMap<>(payload);

        returnValue.putIfAbsent(key.getHashField(), key.getHashValue());
        key.getRangeField().ifPresent(rangeField -> returnValue.putIfAbsent(rangeField, key.getRangeKey().getValue()));

        try {
            table.put(keyArray, objectMapper.writeValueAsBytes(returnValue));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    @Override
    public Map<String, Object> updateItem(final String tableName,
                                          final MapStoreKey key,
                                          final Map<String, Object> payload) {
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
            table.put(keyArray, objectMapper.writeValueAsBytes(returnValue));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    @VisibleForTesting
    Object[] convertSimpleKeyToArray(final MapStoreKey key) {
        return key.getRangeField().map(rangeField -> new Object[]{key.getHashValue(), key.getRangeKey().getValue()})
                .orElseGet(() -> new Object[]{key.getHashValue()});
    }

    @VisibleForTesting
    BTreeMap<Object[], byte[]> getTable(final String tableName, final MapStoreKey key) {
        final Optional<Serializer> rangeKeySerializer = Optional.ofNullable(key.getRangeKey().getValue())
                .map(rangeValue -> (Serializer) SerializerUtils.serializerForClass(rangeValue.getClass()));
        final SerializerArrayTuple storeKeySerializer = rangeKeySerializer
                .map(rangeKeySer -> new SerializerArrayTuple(Serializer.STRING, rangeKeySer))
                .orElseGet(() -> new SerializerArrayTuple(Serializer.STRING));

        return mapDB.treeMap(tableName)
                .keySerializer(storeKeySerializer)
                .valueSerializer(Serializer.BYTE_ARRAY)
                .createOrOpen();
    }

    @VisibleForTesting
    Comparator<Map<String, Object>> valueComparator(final MapStoreKey key) {
        return (m1, m2) -> {
            // If there's no range key than order doesn't matter
            if (!key.getRangeField().isPresent()) {
                return 0;
            }

            // Otherwise, sort by the range field values
            final String rangeField = key.getRangeField().get();
            return ((Comparable) m1.get(rangeField)).compareTo(m2.get(rangeField));
        };
    }

    @VisibleForTesting
    Function<byte[], Map<String, Object>> valueMapper() {
        return value -> {
            try {
                return objectMapper.readValue(value, new TypeReference<Map<String, Object>>(){});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
