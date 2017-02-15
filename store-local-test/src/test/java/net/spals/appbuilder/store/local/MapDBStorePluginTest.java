package net.spals.appbuilder.store.local;

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
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
                {new StoreKey.Builder().setHashField("myHashField").setHashValue("myHashValue").build()},
                // Case: Hash and range key
                {new StoreKey.Builder().setHashField("myHashField").setHashValue("myHashValue")
                    .setRangeField("myRangeField").setRangeValue("myRangeValue").build()},
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
                {new StoreKey.Builder().setHashField("myHashField").setHashValue("myHashValue").build(),
                    ImmutableMap.of("key", "value"),
                    ImmutableMap.of("myHashField", "myHashValue", "key", "value")},
                {new StoreKey.Builder().setHashField("myHashField").setHashValue("myHashValue")
                        .setRangeField("myRangeField").setRangeValue("myRangeValue").build(),
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

    @Test
    public void testUpdateItemAsPutItem() throws IOException {
        // Just for kicks, ensure that fileDBs work too
        final Path dbDir = Files.createTempDirectory(MapDBStorePluginTest.class.getSimpleName());
        final String dbFilePath = dbDir.resolve(UUID.randomUUID() + ".db").toString();

        final DB fileDB = DBMaker.fileDB(dbFilePath).make();
        final StorePlugin storePlugin = new MapDBStorePlugin(fileDB);

        final StoreKey storeKey = new StoreKey.Builder().setHashField("myHashField").setHashValue("myHashValue").build();
        final Map<String, Object> payload = ImmutableMap.of("key", "value");
        final Map<String, Object> expectedResult = ImmutableMap.of("myHashField", "myHashValue", "key", "value");

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

        final StoreKey storeKey = new StoreKey.Builder().setHashField("myHashField").setHashValue("myHashValue")
                .setRangeField("myRangeField").setRangeValue("myRangeValue").build();
        storePlugin.putItem("myTable", storeKey, ImmutableMap.of("key", "value"));

        assertThat(storePlugin.updateItem("myTable", storeKey, payload), is(expectedResult));
        assertThat(storePlugin.getItem("myTable", storeKey), is(Optional.of(expectedResult)));
    }
}
