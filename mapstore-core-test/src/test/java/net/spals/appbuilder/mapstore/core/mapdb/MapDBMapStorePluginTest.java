package net.spals.appbuilder.mapstore.core.mapdb;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.spals.appbuilder.mapstore.core.MapStorePlugin;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
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

import static net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.*;
import static net.spals.appbuilder.mapstore.core.model.TwoValueMapRangeKey.between;
import static net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.all;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link MapDBMapStorePlugin}
 *
 * @author tkral
 */
public class MapDBMapStorePluginTest {

    @DataProvider
    Object[][] emptyGetProvider() {
        return new Object[][] {
                // Case: Hash-only key
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue").build()},
                // Case: Hash and range key
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                    .setRange("myRangeField", equalTo("myRangeValue")).build()},
        };
    }

    @Test(dataProvider = "emptyGetProvider")
    public void testEmptyGet(final MapStoreKey storeKey) throws IOException {
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(DBMaker.memoryDB().make());
        assertThat(storePlugin.getItem("myTable", storeKey), is(Optional.empty()));
    }

    @DataProvider
    Object[][] putItemProvider() {
        return new Object[][] {
                // Case: Hash-only key
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue").build(),
                    ImmutableMap.of("key", "value"),
                    ImmutableMap.of("myHashField", "myHashValue", "key", "value")},
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                    .setRange("myRangeField", equalTo("myRangeValue")).build(),
                    ImmutableMap.of("key", "value"),
                    ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue", "key", "value")},
        };
    }

    @Test(dataProvider = "putItemProvider")
    public void testPutItem(final MapStoreKey storeKey,
                            final Map<String, Object> payload,
                            final Map<String, Object> expectedResult) {
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(DBMaker.memoryDB().make());
        assertThat(storePlugin.putItem("myTable", storeKey, payload), is(expectedResult));
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
                               final Map<String, Object> expectedResult) throws IOException {
        // Just for kicks, ensure that fileDBs work too
        final Path dbDir = Files.createTempDirectory(MapDBMapStorePluginTest.class.getSimpleName());
        final String dbFilePath = dbDir.resolve(UUID.randomUUID() + ".db").toString();

        final DB fileDB = DBMaker.fileDB(dbFilePath).make();
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(fileDB);

        final MapStoreKey storeKey = new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
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
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", all()).build(),
                        ImmutableList.of(result.apply(1), result.apply(2), result.apply(3), result.apply(4))},
                // Case: Between different values
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", between(2, 4)).build(),
                        ImmutableList.of(result.apply(2), result.apply(3), result.apply(4))},
                // Case: Between same value
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", between(2, 2)).build(),
                        ImmutableList.of(result.apply(2))},
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", equalTo(1)).build(),
                    ImmutableList.of(result.apply(1))},
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", greaterThan(2)).build(),
                        ImmutableList.of(result.apply(3), result.apply(4))},
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", greaterThanOrEqualTo(2)).build(),
                        ImmutableList.of(result.apply(2), result.apply(3), result.apply(4))},
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", lessThan(3)).build(),
                        ImmutableList.of(result.apply(1), result.apply(2))},
                {new MapStoreKey.Builder().setHash("myHashField", "myHashValue")
                        .setRange("myRangeField", lessThanOrEqualTo(3)).build(),
                        ImmutableList.of(result.apply(1), result.apply(2), result.apply(3))},
        };
    }

    @Test(dataProvider = "getItemsProvider")
    public void testGetItems(final MapStoreKey storeKey, final List<Map<String, Object>> expectedResults) {
        final MapStorePlugin storePlugin = new MapDBMapStorePlugin(DBMaker.memoryDB().make());
        final Function<Integer, MapStoreKey> keyFunction = i -> new MapStoreKey.Builder()
                .setHash("myHashField", "myHashValue").setRange("myRangeField", equalTo(i)).build();
        final Map<String, Object> payload = ImmutableMap.of("key", "value");

        storePlugin.putItem("myTable", keyFunction.apply(1), payload);
        storePlugin.putItem("myTable", keyFunction.apply(2), payload);
        storePlugin.putItem("myTable", keyFunction.apply(3), payload);
        storePlugin.putItem("myTable", keyFunction.apply(4), payload);

        assertThat(storePlugin.getItems("myTable", storeKey), contains(expectedResults.stream().toArray()));
    }
}
