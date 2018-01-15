package net.spals.appbuilder.mapstore.core.mapdb;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.mapstore.core.model.MapStoreIndexName;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import org.mapdb.DB;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author tkral
 */
@AutoValue
abstract class MapDBIndexMetadata {

    static MapDBIndexMetadata indexMetadata(
        final MapStoreIndexName indexName,
        final MapStoreTableKey indexKey
    ) {
        return new AutoValue_MapDBIndexMetadata(indexName, indexKey);
    }

    static MapDBIndexMetadata findIndexMetadata(
        final DB mapDB,
        final MapStoreIndexName indexName
    ) {
        final Optional<String> indexMetadataStrOpt = StreamSupport.stream(mapDB.getAllNames().spliterator(), false)
            .filter(name -> name.startsWith(indexName.toString() + "?"))
            .findAny();

        checkArgument(indexMetadataStrOpt.isPresent(),
            "No index with name %s exists.", indexName.toString());
        return fromString(indexMetadataStrOpt.get());
    }

    static Set<MapDBIndexMetadata> findIndexMetadata(
        final DB mapDB,
        final String tableName
    ) {
        return StreamSupport.stream(mapDB.getAllNames().spliterator(), false)
            .filter(name -> name.startsWith(tableName + "."))
            .map(name -> fromString(name))
            .collect(Collectors.toSet());
    }

    static MapDBIndexMetadata fromString(final String indexMetadataStr) {
        final String[] parsedIndexMetadataStr = checkNotNull(indexMetadataStr).split("\\?", 2);

        final MapStoreIndexName indexName = MapStoreIndexName.fromString(parsedIndexMetadataStr[0]);
        final String[] parsedKeys = parsedIndexMetadataStr[1].split("&", 2);

        final MapStoreTableKey.Builder indexKeyBuilder = new MapStoreTableKey.Builder();

        final String[] parsedHashKey = parsedKeys[0].split("=", 2);
        indexKeyBuilder.setHashField(parsedHashKey[0]);
        indexKeyBuilder.setHashFieldType(loadKeyClass(parsedHashKey[1]));

        Optional.of(parsedKeys).filter(keys -> keys.length > 1)
            .ifPresent(keys -> {
                final String[] parsedRangeKey = keys[1].split("=", 2);
                indexKeyBuilder.setRangeField(parsedRangeKey[0]);
                indexKeyBuilder.setRangeFieldType((Class<? extends Comparable>)loadKeyClass(parsedRangeKey[1]));
            });

        return new AutoValue_MapDBIndexMetadata(indexName, indexKeyBuilder.build());
    }

    private static Class<?> loadKeyClass(final String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException();
        }
    }

    abstract MapStoreIndexName getIndexName();

    abstract MapStoreTableKey getIndexKey();

    @Override
    public String toString() {
        final StringBuilder keyNameBuilder = new StringBuilder("?")
            .append(getIndexKey().getHashField()).append("=").append(getIndexKey().getHashFieldType().getName());
        getIndexKey().getRangeField().ifPresent(rangeField -> keyNameBuilder.append("&").append(rangeField)
            .append("=").append(getIndexKey().getRangeFieldType().get().getName()));
        final String keyName = keyNameBuilder.toString();

        return getIndexName().toString() + keyName;
    }
}
