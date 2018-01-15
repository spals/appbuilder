package net.spals.appbuilder.mapstore.core.mapdb;

import com.google.common.collect.ImmutableMap;
import net.spals.appbuilder.mapstore.core.MapStoreIndexPlugin;
import net.spals.appbuilder.mapstore.core.MapStorePlugin;
import net.spals.appbuilder.mapstore.core.model.MapStoreIndexName;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static net.spals.appbuilder.mapstore.core.mapdb.MapDBIndexMetadata.findIndexMetadata;
import static net.spals.appbuilder.mapstore.core.mapdb.MapDBIndexMetadata.indexMetadata;
import static net.spals.appbuilder.mapstore.core.model.MapQueryOptions.defaultOptions;
import static net.spals.appbuilder.mapstore.core.model.MapStoreIndexName.indexName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link MapDBMapStoreIndexPlugin}.
 *
 * @author tkral
 */
public class MapDBMapStoreIndexPluginTest {

    private static final MapStoreIndexName INDEX_NAME = indexName("myTable", "myIndex");

    private static final MapStoreTableKey HASH_ONLY_INDEX_KEY =
        new MapStoreTableKey.Builder().setHash("indexHashField", String.class).build();
    private static final MapStoreTableKey HASH_ONLY_TABLE_KEY =
        new MapStoreTableKey.Builder().setHash("tableHashField", String.class).build();

    private static final MapStoreTableKey HASH_RANGE_INDEX_KEY =
        new MapStoreTableKey.Builder().setHash("indexHashField", String.class)
            .setRange("indexRangeField", Integer.class).build();
    private static final MapStoreTableKey HASH_RANGE_TABLE_KEY =
        new MapStoreTableKey.Builder().setHash("tableHashField", String.class)
            .setRange("tableRangeField", Integer.class).build();

    @DataProvider
    Object[][] tableKeyProvider() {
        return new Object[][] {
            {HASH_ONLY_TABLE_KEY, HASH_ONLY_INDEX_KEY},
            {HASH_RANGE_TABLE_KEY, HASH_RANGE_INDEX_KEY},
            {HASH_RANGE_TABLE_KEY, HASH_ONLY_INDEX_KEY},
        };
    }

    @Test(dataProvider = "tableKeyProvider")
    public void testCreateIndex(final MapStoreTableKey tableKey, final MapStoreTableKey indexKey) {
        final DB mapDB = DBMaker.memoryDB().make();
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(mapDB);
        final MapStoreIndexPlugin storeIndexPlugin = new MapDBMapStoreIndexPlugin(mapDB, storePlugin);

        storePlugin.createTable(INDEX_NAME.getTableName(), tableKey);
        storeIndexPlugin.createIndex(INDEX_NAME, indexKey);

        final Set<MapDBIndexMetadata> myTableIndexes = findIndexMetadata(mapDB, "myTable");
        assertThat(myTableIndexes, contains(indexMetadata(INDEX_NAME, indexKey)));
    }

    @Test
    public void testEmptyGetItem() {
        final DB mapDB = DBMaker.memoryDB().make();
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(mapDB);
        final MapStoreIndexPlugin storeIndexPlugin = new MapDBMapStoreIndexPlugin(mapDB, storePlugin);

        storePlugin.createTable(INDEX_NAME.getTableName(), HASH_ONLY_TABLE_KEY);
        storeIndexPlugin.createIndex(INDEX_NAME, HASH_ONLY_INDEX_KEY);

        final MapStoreKey indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build();
        assertThat(storeIndexPlugin.getItem(INDEX_NAME, indexItemKey), is(Optional.empty()));
    }

    @Test
    public void testEmptyGetItems() {
        final DB mapDB = DBMaker.memoryDB().make();
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(mapDB);
        final MapStoreIndexPlugin storeIndexPlugin = new MapDBMapStoreIndexPlugin(mapDB, storePlugin);

        storePlugin.createTable(INDEX_NAME.getTableName(), HASH_ONLY_TABLE_KEY);
        storeIndexPlugin.createIndex(INDEX_NAME, HASH_ONLY_INDEX_KEY);

        final MapStoreKey indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build();
        assertThat(storeIndexPlugin.getItems(INDEX_NAME, indexItemKey, defaultOptions()), empty());
    }

    @Test
    public void testFillIndexAfterCreate() {
        final DB mapDB = DBMaker.memoryDB().make();
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(mapDB);
        final MapStoreIndexPlugin storeIndexPlugin = new MapDBMapStoreIndexPlugin(mapDB, storePlugin);

        storePlugin.createTable(INDEX_NAME.getTableName(), HASH_ONLY_TABLE_KEY);

        final MapStoreKey tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build();
        storePlugin.putItem(INDEX_NAME.getTableName(), tableItemKey, ImmutableMap.of("indexHashField", "index"));

        storeIndexPlugin.createIndex(INDEX_NAME, HASH_ONLY_INDEX_KEY);

        final MapStoreKey indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build();
        final Optional<Map<String, Object>> indexItem = storeIndexPlugin.getItem(INDEX_NAME, indexItemKey);
        assertThat(indexItem, not(Optional.empty()));
        assertThat(indexItem.get(), is(ImmutableMap.of("tableHashField", "table", "indexHashField", "index")));
    }

    @Test
    public void testGetItem() {
        final DB mapDB = DBMaker.memoryDB().make();
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(mapDB);
        final MapStoreIndexPlugin storeIndexPlugin = new MapDBMapStoreIndexPlugin(mapDB, storePlugin);

        storePlugin.createTable(INDEX_NAME.getTableName(), HASH_ONLY_TABLE_KEY);
        storeIndexPlugin.createIndex(INDEX_NAME, HASH_ONLY_INDEX_KEY);

        final MapStoreKey tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build();
        storePlugin.putItem(INDEX_NAME.getTableName(), tableItemKey, ImmutableMap.of("indexHashField", "index"));

        final MapStoreKey indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build();
        final Optional<Map<String, Object>> indexItem = storeIndexPlugin.getItem(INDEX_NAME, indexItemKey);
        assertThat(indexItem, not(Optional.empty()));
        assertThat(indexItem.get(), is(ImmutableMap.of("tableHashField", "table", "indexHashField", "index")));
    }

    @Test
    public void testUpdateItem() {
        final DB mapDB = DBMaker.memoryDB().make();
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(mapDB);
        final MapStoreIndexPlugin storeIndexPlugin = new MapDBMapStoreIndexPlugin(mapDB, storePlugin);

        storePlugin.createTable(INDEX_NAME.getTableName(), HASH_ONLY_TABLE_KEY);
        storeIndexPlugin.createIndex(INDEX_NAME, HASH_ONLY_INDEX_KEY);

        final MapStoreKey tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build();
        storePlugin.putItem(INDEX_NAME.getTableName(), tableItemKey, ImmutableMap.of("indexHashField", "index"));
        storePlugin.updateItem(INDEX_NAME.getTableName(), tableItemKey, ImmutableMap.of("key", "value"));

        final MapStoreKey indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build();
        final Optional<Map<String, Object>> indexItem = storeIndexPlugin.getItem(INDEX_NAME, indexItemKey);
        assertThat(indexItem, not(Optional.empty()));
        assertThat(indexItem.get(), is(ImmutableMap.of("tableHashField", "table", "indexHashField", "index", "key", "value")));
    }

    @Test
    public void testDeleteItem() {
        final DB mapDB = DBMaker.memoryDB().make();
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(mapDB);
        final MapStoreIndexPlugin storeIndexPlugin = new MapDBMapStoreIndexPlugin(mapDB, storePlugin);

        storePlugin.createTable(INDEX_NAME.getTableName(), HASH_ONLY_TABLE_KEY);
        storeIndexPlugin.createIndex(INDEX_NAME, HASH_ONLY_INDEX_KEY);

        final MapStoreKey tableItemKey = new MapStoreKey.Builder().setHash("tableHashField", "table").build();
        storePlugin.putItem(INDEX_NAME.getTableName(), tableItemKey, ImmutableMap.of("indexHashField", "index"));
        storePlugin.deleteItem(INDEX_NAME.getTableName(), tableItemKey);

        final MapStoreKey indexItemKey = new MapStoreKey.Builder().setHash("indexHashField", "index").build();
        final Optional<Map<String, Object>> indexItem = storeIndexPlugin.getItem(INDEX_NAME, indexItemKey);
        assertThat(indexItem, is(Optional.empty()));
    }
}
