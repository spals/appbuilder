package net.spals.appbuilder.mapstore.core;

import com.google.common.collect.ImmutableMap;
import net.spals.appbuilder.mapstore.core.MapStoreProvider.DelegatingMapStore;
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions;
import net.spals.appbuilder.mapstore.core.model.MapQueryOptions.Order;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.googlecode.catchexception.CatchException.*;
import static net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.equalTo;
import static net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.greaterThan;
import static net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DelegatingMapStore}
 *
 * @author tkral
 */
public class DelegatingMapStoreTest {

    @Test
    public void testCheckPutItem() {
        final DelegatingMapStore delegatingMapStore = new DelegatingMapStore(mock(MapStorePlugin.class));

        catchException(() -> delegatingMapStore.checkPutItem(Collections.singletonMap("key", "value")));
        assertThat(caughtException(), is(nullValue()));
    }

    @DataProvider
    Object[][] checkPutItemIllegalProvider() {
        return new Object[][] {
            // Case: Null value
            {Collections.singletonMap("key", null)},
            // Case: Empty string value
            {Collections.singletonMap("key", "")},
        };
    }

    @Test(dataProvider = "checkPutItemIllegalProvider")
    public void testCheckPutItemIllegal(final Map<String, Object> payload) {
        final DelegatingMapStore delegatingMapStore = new DelegatingMapStore(mock(MapStorePlugin.class));
        verifyException(() -> delegatingMapStore.checkPutItem(payload), IllegalArgumentException.class);
    }

    @DataProvider
    Object[][] checkSingleItemKeyProvider() {
        return new Object[][] {
            {
                new MapStoreKey.Builder()
                    .setHash("myHashField", "myHashValue")
                    .build()
            },
            {
                new MapStoreKey.Builder()
                    .setHash("myHashField", "myHashValue")
                    .setRange("myRangeField", equalTo("myRangeValue"))
                    .build()
            },
        };
    }

    @Test(dataProvider = "checkSingleItemKeyProvider")
    public void testCheckSingleItemKey(final MapStoreKey key) {
        final DelegatingMapStore delegatingMapStore = new DelegatingMapStore(mock(MapStorePlugin.class));

        catchException(() -> delegatingMapStore.checkSingleItemKey(key));
        assertThat(caughtException(), is(nullValue()));
    }

    @DataProvider
    Object[][] checkSingleItemKeyIllegalProvider() {
        return new Object[][] {
            {
                new MapStoreKey.Builder()
                    .setHash("myHashField", "myHashValue")
                    .setRange("myRangeField", all())
                    .build()
            },
            {
                new MapStoreKey.Builder()
                    .setHash("myHashField", "myHashValue")
                    .setRange("myRangeField", greaterThan(1L))
                    .build()
            },
        };
    }

    @Test(dataProvider = "checkSingleItemKeyIllegalProvider")
    public void testCheckSingleItemKeyIllegal(final MapStoreKey key) {
        final DelegatingMapStore delegatingMapStore = new DelegatingMapStore(mock(MapStorePlugin.class));
        verifyException(() -> delegatingMapStore.checkSingleItemKey(key), IllegalArgumentException.class);
    }

    @DataProvider
    Object[][] checkWriteItemProvider() {
        return new Object[][] {
            {new MapStoreKey.Builder()
                .setHash("myHashField", "myHashValue").build(),
                Collections.singletonMap("myHashField", "myHashValue")},
            {new MapStoreKey.Builder()
                .setHash("myHashField", "myHashValue")
                .setRange("myRangeKey", equalTo("myRangeValue")).build(),
                ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue")},
        };
    }

    @Test(dataProvider = "checkWriteItemProvider")
    public void testCheckWriteItem(final MapStoreKey key, final Map<String, Object> payload) {
        final DelegatingMapStore delegatingMapStore = new DelegatingMapStore(mock(MapStorePlugin.class));

        catchException(() -> delegatingMapStore.checkWriteItem(key, payload));
        assertThat(caughtException(), is(nullValue()));
    }

    @DataProvider
    Object[][] checkWriteItemIllegalProvider() {
        return new Object[][] {
            // Case: Empty payload
            {new MapStoreKey.Builder().setHash("myHashField", "myHashValue").build(),
                Collections.emptyMap()},
            // Case: Mismatched hash field in key
            {new MapStoreKey.Builder().setHash("myHashField", "myHashValue").build(),
                Collections.singletonMap("myHashField", "value")},
            // Case: Mismatched range field in key
            {new MapStoreKey.Builder()
                .setHash("myHashField", "myHashValue")
                .setRange("myRangeField", equalTo("myRangeValue")).build(),
                ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "value")},
            // Case: Illegal map range key
            {new MapStoreKey.Builder()
                .setHash("myHashField", "myHashValue")
                .setRange("myRangeField", greaterThan("myRangeValue")).build(),
                ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue")},
        };
    }

    @Test(dataProvider = "checkWriteItemIllegalProvider")
    public void testCheckWriteItemIllegal(final MapStoreKey key, final Map<String, Object> payload) {
        final DelegatingMapStore delegatingMapStore = new DelegatingMapStore(mock(MapStorePlugin.class));
        verifyException(() -> delegatingMapStore.checkWriteItem(key, payload), IllegalArgumentException.class);
    }

    @Test
    public void testCreateTable() {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        final MapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final String tableName = "myTable";
        final MapStoreTableKey tableKey = new MapStoreTableKey.Builder()
            .setHash("myHashField", String.class)
            .build();
        delegatingMapStore.createTable(tableName, tableKey);

        verify(pluginDelegate).createTable(same(tableName), same(tableKey));
    }

    @Test
    public void testDeleteItem() {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        final MapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final String tableName = "myTable";
        final MapStoreKey tableKey = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", equalTo("myRangeValue"))
            .build();
        delegatingMapStore.deleteItem(tableName, tableKey);

        verify(pluginDelegate).deleteItem(same(tableName), same(tableKey));
    }

    @Test
    public void testDeleteItemIllegalKey() {
        final MapStore delegatingMapStore = new DelegatingMapStore(mock(MapStorePlugin.class));

        final String tableName = "myTable";
        final MapStoreKey tableKey = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", greaterThan(1L))
            .build();
        verifyException(() -> delegatingMapStore.deleteItem(tableName, tableKey), IllegalArgumentException.class);
    }

    @Test
    public void testDropTable() {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        final MapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final String tableName = "myTable";
        delegatingMapStore.dropTable(tableName);

        verify(pluginDelegate).dropTable(same(tableName));
    }

    @Test
    public void testGetAllItems() {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        final MapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final String tableName = "myTable";
        delegatingMapStore.getAllItems(tableName);

        verify(pluginDelegate).getAllItems(same(tableName));
    }

    @Test
    public void testGetItem() {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        final MapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final String tableName = "myTable";
        final MapStoreKey tableKey = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", equalTo("myRangeValue"))
            .build();
        delegatingMapStore.getItem(tableName, tableKey);

        verify(pluginDelegate).getItem(same(tableName), same(tableKey));
    }

    @Test
    public void testGetItemIllegalKey() {
        final MapStore delegatingMapStore = new DelegatingMapStore(mock(MapStorePlugin.class));

        final String tableName = "myTable";
        final MapStoreKey tableKey = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", greaterThan(1L))
            .build();
        verifyException(() -> delegatingMapStore.getItem(tableName, tableKey), IllegalArgumentException.class);
    }

    @DataProvider
    Object[][] getMaxItemProvider() {
        return new Object[][] {
            {Collections.emptyList(), Optional.empty()},
            {Collections.singletonList(Collections.<String, Object>emptyMap()),
                Optional.of(Collections.<String, Object>emptyMap())},
        };
    }

    @Test(dataProvider = "getMaxItemProvider")
    public void testGetMaxItem(final List<Map<String, Object>> returnedMaxItem,
                               final Optional<Map<String, Object>> expectedResult) {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        when(pluginDelegate.getItems(anyString(), any(MapStoreKey.class), any(MapQueryOptions.class)))
            .thenReturn(returnedMaxItem);
        final DelegatingMapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final String tableName = "myTable";
        final MapStoreKey tableKey = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", max())
            .build();
        final Optional<Map<String, Object>> result = delegatingMapStore.getItem(tableName, tableKey);

        final MapStoreKey expectedMaxItemKey = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", all())
            .build();
        final MapQueryOptions expectedQueryOptions = new MapQueryOptions.Builder()
            .setOrder(Order.DESC).setLimit(1).build();
        // Verify that we've called get items with the expected parameters
        verify(pluginDelegate).getItems(same(tableName), eq(expectedMaxItemKey), eq(expectedQueryOptions));

        assertThat(result, is(expectedResult));
    }

    @DataProvider
    Object[][] getMinItemProvider() {
        return new Object[][] {
            {Collections.emptyList(), Optional.empty()},
            {Collections.singletonList(Collections.<String, Object>emptyMap()),
                Optional.of(Collections.<String, Object>emptyMap())},
        };
    }

    @Test(dataProvider = "getMinItemProvider")
    public void testGetMinItem(final List<Map<String, Object>> returnedMinItem,
                               final Optional<Map<String, Object>> expectedResult) {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        when(pluginDelegate.getItems(anyString(), any(MapStoreKey.class), any(MapQueryOptions.class)))
            .thenReturn(returnedMinItem);
        final DelegatingMapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final String tableName = "myTable";
        final MapStoreKey tableKey = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", min())
            .build();
        final Optional<Map<String, Object>> result = delegatingMapStore.getItem(tableName, tableKey);

        final MapStoreKey expectedMaxItemKey = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", all())
            .build();
        final MapQueryOptions expectedQueryOptions = new MapQueryOptions.Builder()
            .setOrder(Order.ASC).setLimit(1).build();
        // Verify that we've called get items with the expected parameters
        verify(pluginDelegate).getItems(same(tableName), eq(expectedMaxItemKey), eq(expectedQueryOptions));

        assertThat(result, is(expectedResult));
    }

    @Test
    public void testPutItem() {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        final MapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final MapStoreKey key = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", equalTo("myRangeValue"))
            .build();
        final Map<String, Object> payload = ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue");
        delegatingMapStore.putItem("myTable", key, payload);

        verify(pluginDelegate).putItem(eq("myTable"), same(key), same(payload));
    }

    @Test
    public void testUpdateItemNoItemPresent() {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        when(pluginDelegate.getItem(anyString(), any(MapStoreKey.class))).thenReturn(Optional.empty());
        final MapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final MapStoreKey key = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", equalTo("myRangeValue"))
            .build();
        final Map<String, Object> payload = ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue");
        delegatingMapStore.updateItem("myTable", key, payload);
        // When no item is present, then updateItem turns into putItem
        verify(pluginDelegate).putItem(eq("myTable"), same(key), same(payload));
    }

    @Test
    public void testUpdateItemItemPresent() {
        final MapStorePlugin pluginDelegate = mock(MapStorePlugin.class);
        when(pluginDelegate.getItem(anyString(), any(MapStoreKey.class)))
            .thenReturn(Optional.of(Collections.singletonMap("key", "value")));
        final MapStore delegatingMapStore = new DelegatingMapStore(pluginDelegate);

        final MapStoreKey key = new MapStoreKey.Builder()
            .setHash("myHashField", "myHashValue")
            .setRange("myRangeField", equalTo("myRangeValue"))
            .build();
        final Map<String, Object> payload = ImmutableMap.of("myHashField", "myHashValue", "myRangeField", "myRangeValue");
        delegatingMapStore.updateItem("myTable", key, payload);
        // When item is present, then updateItem runs updateItem
        verify(pluginDelegate).updateItem(eq("myTable"), same(key), same(payload));
    }
}
