package net.spals.appbuilder.mapstore.core.model;

import com.google.auto.value.AutoValue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author tkral
 */
@AutoValue
public abstract class MapStoreIndexName {

    public static MapStoreIndexName indexName(
        final String tableName,
        final String indexName
    ) {
        return new AutoValue_MapStoreIndexName(tableName, indexName);
    }

    public static MapStoreIndexName fromString(final String indexNameStr) {
        final String[] parsedIndexNameStr = checkNotNull(indexNameStr).split("\\.", 2);
        return new AutoValue_MapStoreIndexName(parsedIndexNameStr[0], parsedIndexNameStr[1]);
    }

    public abstract String getTableName();

    public abstract String getIndexName();

    @Override
    public String toString() {
        return getTableName() + "." + getIndexName();
    }
}
