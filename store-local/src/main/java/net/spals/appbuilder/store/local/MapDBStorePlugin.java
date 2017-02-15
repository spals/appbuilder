package net.spals.appbuilder.store.local;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.store.core.StorePlugin;
import net.spals.appbuilder.store.core.model.StoreKey;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArrayTuple;
import org.mapdb.serializer.SerializerUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author tkral
 */
@AutoBindInMap(baseClass = StorePlugin.class, key = "local")
class MapDBStorePlugin implements StorePlugin {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DB mapDB;

    MapDBStorePlugin(final DB mapDB) {
        this.mapDB = mapDB;
    }

    @Override
    public void deleteItem(final String tableName,
                           final StoreKey key) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertKeyToArray(key);

        table.remove(keyArray);
    }

    @Override
    public Optional<Map<String, Object>> getItem(final String tableName,
                                                 final StoreKey key) {
        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertKeyToArray(key);

        final Optional<byte[]> valueArray = Optional.ofNullable(table.get(keyArray));
        return valueArray.map(vArray -> {
            try {
                return objectMapper.readValue(vArray, new TypeReference<Map<String, Object>>(){});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Map<String, Object> putItem(final String tableName,
                                       final StoreKey key,
                                       final Map<String, Object> payload) {
        checkArgument(!payload.isEmpty(), "Cannot put item with empty payload");
        checkKeyField(key.getHashField(), key.getHashValue(), payload);
        key.getRangeField().ifPresent(rangeField -> checkKeyField(rangeField, key.getRangeValue().get(), payload));

        // Null or empty values have special semantics in updateItem so we'll disallow them here.
        final Set<String> nullValueKeys = payload.entrySet().stream()
                .filter(isNullOrEmptyEntry())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        checkArgument(nullValueKeys.isEmpty(), "The following keys have null or empty values: %s", nullValueKeys);

        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertKeyToArray(key);
        final Map<String, Object> returnValue = new TreeMap<>(payload);

        returnValue.putIfAbsent(key.getHashField(), key.getHashValue());
        key.getRangeField().ifPresent(rangeField -> returnValue.putIfAbsent(rangeField, key.getRangeValue().get()));

        try {
            table.put(keyArray, objectMapper.writeValueAsBytes(returnValue));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return returnValue;
    }

    @Override
    public Map<String, Object> updateItem(final String tableName,
                                          final StoreKey key,
                                          final Map<String, Object> payload) {
        checkArgument(!payload.isEmpty(), "Cannot update item with empty payload");
        checkKeyField(key.getHashField(), key.getHashValue(), payload);
        key.getRangeField().ifPresent(rangeField -> checkKeyField(rangeField, key.getRangeValue().get(), payload));

        final Optional<Map<String, Object>> item = getItem(tableName, key);
        // If no item is present at the given key, then updateItem takes on putItem semantics
        if (!item.isPresent()) {
            return putItem(tableName, key, payload);
        }

        final BTreeMap<Object[], byte[]> table = getTable(tableName, key);
        final Object[] keyArray = convertKeyToArray(key);
        final Map<String, Object> returnValue = new TreeMap<>(item.get());

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
    void checkKeyField(final String keyField, final Object keyValue, final Map<String, Object> payload) {
        checkArgument(!payload.containsKey(keyField) || keyValue.equals(payload.get(keyField)),
                "Mismatched key value (%s) and payload field value (%s)",
                keyValue, payload.get(keyField));
    }

    @VisibleForTesting
    Object[] convertKeyToArray(final StoreKey key) {
        return key.getRangeValue().map(rangeValue -> new Object[]{key.getHashValue(), rangeValue})
                .orElseGet(() -> new Object[]{key.getHashValue()});
    }

    @VisibleForTesting
    BTreeMap<Object[], byte[]> getTable(final String tableName, final StoreKey key) {
        final SerializerArrayTuple storeKeySerializer = key.getRangeValue()
                .map(rangeValue -> new SerializerArrayTuple(Serializer.STRING, SerializerUtils.serializerForClass(rangeValue.getClass())))
                .orElseGet(() -> new SerializerArrayTuple(Serializer.STRING));

        return mapDB.treeMap(tableName).keySerializer(storeKeySerializer).valueSerializer(Serializer.BYTE_ARRAY).createOrOpen();
    }

    @VisibleForTesting
    Predicate<Map.Entry> isNullOrEmptyEntry() {
        return entry -> entry.getValue() == null || "".equals(entry.getValue());
    }
}
