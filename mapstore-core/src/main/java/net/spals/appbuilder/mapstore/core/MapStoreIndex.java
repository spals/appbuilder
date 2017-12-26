package net.spals.appbuilder.mapstore.core;

import net.spals.appbuilder.mapstore.core.model.MapQueryOptions;
import net.spals.appbuilder.mapstore.core.model.MapStoreIndexName;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A service which represents an index of a {@link MapStore}.
 * <p>
 * Note that this is a separate service because not all
 * {@link MapStorePlugin} implementations may support indexes.
 * Also note, that indexes are read only. They will always
 * be populated automatically with writes to {@link MapStore}.
 *
 * @author tkral
 */
public interface MapStoreIndex extends Closeable {

    /**
     * Creates an index with the given key.
     *
     * Note that the table must already exist.
     *
     * @return true iff the creation was successful
     */
    boolean createIndex(
        MapStoreIndexName indexName,
        MapStoreTableKey indexKey
    );

    /**
     * Drops an index.
     *
     * @return true iff the drop was successful
     */
    boolean dropIndex(MapStoreIndexName indexName);

    /**
     * Retrieves an item from the given index
     * with the given key.
     *
     * Returns {@link Optional#empty()} if no
     * item exists with the given key.
     */
    Optional<Map<String, Object>> getItem(
        MapStoreIndexName indexName,
        MapStoreKey key
    );

    /**
     * Queries all items from the given index
     * which match the given {@link MapStoreKey}
     * range key operator.
     */
    List<Map<String, Object>> getItems(
        MapStoreIndexName indexName,
        MapStoreKey key,
        MapQueryOptions options
    );
}
