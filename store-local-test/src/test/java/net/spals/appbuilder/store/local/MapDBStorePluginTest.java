package net.spals.appbuilder.store.local;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.spals.appbuilder.store.core.StorePlugin;
import net.spals.appbuilder.store.core.model.StoreKey;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static net.spals.appbuilder.store.core.model.SingleValueRangeKey.*;
import static net.spals.appbuilder.store.core.model.TwoValueRangeKey.between;
import static net.spals.appbuilder.store.core.model.ZeroValueRangeKey.all;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link MapDBStorePlugin}
 *
 * @author tkral
 */
public class MapDBStorePluginTest {

    @DataProvider
    Object[][] emptyGetProvider() {
        return new Object[][] {
                // Case: Hash-only key
                {new StoreKey.Builder().setHash("myHashField", "myHashValue").build()},
                // Case: Hash and range key
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                    .setRange("myRangeField", equalTo("myRangeValue")).build()},
        };
    }

    @Test(dataProvider = "emptyGetProvider")
    public void testEmptyGet(final StoreKey storeKey) throws IOException {
        final StorePlugin storePlugin = new MapDBStorePlugin(DBMaker.memoryDB().make());
        assertThat(storePlugin.getItem("myTable", storeKey), is(Optional.empty()));
    }

    @DataProvider
    Object[][] putItemProvider() {
        return new Object[][] {
                // Case: Hash-only key
                {new StoreKey.Builder().setHash("myHashField", "myHashValue").build(),
                    ImmutableMap.of("key", "value"),
                    ImmutableMap.of("myHashField", "myHashValue", "key", "value")},
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                    .setRange("myRangeField", equalTo("myRangeValue")).build(),
                    ImmutableMap.of("key", "value"),
                    ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue", "key", "value")},
        };
    }

    @Test(dataProvider = "putItemProvider")
    public void testPutItem(final StoreKey storeKey,
                            final Map<String, Object> payload,
                            final Map<String, Object> expectedResult) {
        final StorePlugin storePlugin = new MapDBStorePlugin(DBMaker.memoryDB().make());
        assertThat(storePlugin.putItem("myTable", storeKey, payload), is(expectedResult));
        assertThat(storePlugin.getItem("myTable", storeKey), is(Optional.of(expectedResult)));
    }

    @Test(dataProvider = "putItemProvider")
    public void testUpdateItemAsPutItem(final StoreKey storeKey,
                                        final Map<String, Object> payload,
                                        final Map<String, Object> expectedResult) throws IOException {
        // Just for kicks, ensure that fileDBs work too
        final Path dbDir = Files.createTempDirectory(MapDBStorePluginTest.class.getSimpleName());
        final String dbFilePath = dbDir.resolve(UUID.randomUUID() + ".db").toString();

        final DB fileDB = DBMaker.fileDB(dbFilePath).make();
        final StorePlugin storePlugin = new MapDBStorePlugin(fileDB);

        assertThat(storePlugin.updateItem("myTable", storeKey, payload), is(expectedResult));
        assertThat(storePlugin.getItem("myTable", storeKey), is(Optional.of(expectedResult)));
    }

    @DataProvider
    Object[][] updateItemProvider() {
        return new Object[][] {
                {ImmutableMap.of("key1", 1),
                    ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue", "key", "value", "key1", 1)},
                {ImmutableMap.of("key", ""),
                    ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue")},
        };
    }

    @Test(dataProvider = "updateItemProvider")
    public void testUpdateItem(final Map<String, Object> payload,
                               final Map<String, Object> expectedResult) {
        final StorePlugin storePlugin = new MapDBStorePlugin(DBMaker.memoryDB().make());

        final StoreKey storeKey = new StoreKey.Builder().setHash("myHashField", "myHashValue")
                .setRange("myRangeField", equalTo("myRangeValue")).build();
        storePlugin.putItem("myTable", storeKey, ImmutableMap.of("key", "value"));

        assertThat(storePlugin.updateItem("myTable", storeKey, payload), is(expectedResult));
        assertThat(storePlugin.getItem("myTable", storeKey), is(Optional.of(expectedResult)));
    }

    @DataProvider
    Object[][] getItemsProvider() {
        final Function<Integer, Map<String, Object>> result = i -> ImmutableMap.of("myHashField", "myHashValue",
                "myRangeField", i, "key", "value");

        return new Object[][] {
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", all()).build(),
                        ImmutableList.of(result.apply(1), result.apply(2), result.apply(3), result.apply(4))},
                // Case: Between different values
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", between(2, 4)).build(),
                        ImmutableList.of(result.apply(2), result.apply(3), result.apply(4))},
                // Case: Between same value
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", between(2, 2)).build(),
                        ImmutableList.of(result.apply(2))},
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", equalTo(1)).build(),
                    ImmutableList.of(result.apply(1))},
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", greaterThan(2)).build(),
                        ImmutableList.of(result.apply(3), result.apply(4))},
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", greaterThanOrEqualTo(2)).build(),
                        ImmutableList.of(result.apply(2), result.apply(3), result.apply(4))},
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", lessThan(3)).build(),
                        ImmutableList.of(result.apply(1), result.apply(2))},
                {new StoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", lessThanOrEqualTo(3)).build(),
                        ImmutableList.of(result.apply(1), result.apply(2), result.apply(3))},
        };
    }

    @Test(dataProvider = "getItemsProvider")
    public void testGetItems(final StoreKey storeKey, final List<Map<String, Object>> expectedResults) {
        final StorePlugin storePlugin = new MapDBStorePlugin(DBMaker.memoryDB().make());
        final Function<Integer, StoreKey> keyFunction = i -> new StoreKey.Builder()
                .setHash("myHashField", "myHashValue").setRange("myRangeField", equalTo(i)).build();
        final Map<String, Object> payload = ImmutableMap.of("key", "value");

        storePlugin.putItem("myTable", keyFunction.apply(1), payload);
        storePlugin.putItem("myTable", keyFunction.apply(2), payload);
        storePlugin.putItem("myTable", keyFunction.apply(3), payload);
        storePlugin.putItem("myTable", keyFunction.apply(4), payload);

        assertThat(storePlugin.getItems("myTable", storeKey), contains(expectedResults.stream().toArray()));
    }
}
