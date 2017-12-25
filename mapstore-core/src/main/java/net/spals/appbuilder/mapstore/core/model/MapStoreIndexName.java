package net.spals.appbuilder.mapstore.core.model;

import com.google.auto.value.AutoValue;

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

    public abstract String getTableName();

    public abstract String getIndexName();

    @Override
    public String toString() {
        return getTableName() + "." + getIndexName();
    }
}
