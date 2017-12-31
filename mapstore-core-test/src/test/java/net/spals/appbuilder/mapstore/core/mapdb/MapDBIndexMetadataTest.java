package net.spals.appbuilder.mapstore.core.mapdb;

import net.spals.appbuilder.mapstore.core.model.MapStoreIndexName;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import org.mapdb.DB;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Set;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static net.spals.appbuilder.mapstore.core.mapdb.MapDBIndexMetadata.*;
import static net.spals.appbuilder.mapstore.core.model.MapStoreIndexName.indexName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link MapDBIndexMetadata}.
 *
 * @author tkral
 */
public class MapDBIndexMetadataTest {

    @DataProvider
    Object[][] indexMetadataProvider() {
        final MapStoreIndexName indexName = indexName("table", "index");
        return new Object[][] {
            // Case: Hash field only
            {indexMetadata(indexName,
                new MapStoreTableKey.Builder().setHash("hashField", String.class).build()),
                "table.index?hashField=java.lang.String"},
            // Case: Hash and range fields
            {indexMetadata(indexName,
                new MapStoreTableKey.Builder().setHash("hashField", String.class).setRange("rangeField", Integer.class).build()),
                "table.index?hashField=java.lang.String&rangeField=java.lang.Integer"},
        };
    }

    @Test(dataProvider = "indexMetadataProvider")
    public void testToString(
        final MapDBIndexMetadata indexMetadata,
        final String expectedString
    ) {
        assertThat(indexMetadata.toString(), is(expectedString));
    }

    @Test(dataProvider = "indexMetadataProvider")
    public void testFromString(
        final MapDBIndexMetadata expectedIndexMetadata,
        final String metadataStr
    ) {
        assertThat(fromString(metadataStr), is(expectedIndexMetadata));
    }

    @Test
    public void testFindIndexMetadataFromIndexNameMissing() {
        final DB mapDB = new MapDBProvider().get();

        catchException(() -> findIndexMetadata(mapDB, MapStoreIndexName.fromString("table.index")));
        assertThat(caughtException(), instanceOf(IllegalArgumentException.class));
    }

    @Test(dataProvider = "indexMetadataProvider")
    public void testFindIndexMetadataFromIndexName(
        final MapDBIndexMetadata expectedIndexMetadata,
        final String metadataStr
    ) {
        final DB mapDB = new MapDBProvider().get();
        mapDB.treeMap(metadataStr).createOrOpen();

        final MapDBIndexMetadata indexMetadata = findIndexMetadata(mapDB, MapStoreIndexName.fromString("table.index"));
        assertThat(indexMetadata, is(expectedIndexMetadata));
    }

    @Test
    public void testFindIndexMetadataFromTableNameMissing() {
        final DB mapDB = new MapDBProvider().get();

        final Set<MapDBIndexMetadata> indexMetadatas = findIndexMetadata(mapDB, "table");
        assertThat(indexMetadatas, emptyCollectionOf(MapDBIndexMetadata.class));
    }

    @Test
    public void testFindIndexMetadataFromTableName() {
        final DB mapDB = new MapDBProvider().get();

        final MapStoreIndexName indexName1 = indexName("table", "index1");
        final MapDBIndexMetadata indexMetadata1 = indexMetadata(indexName1,
            new MapStoreTableKey.Builder().setHash("hashField", String.class).build());
        mapDB.treeMap(indexMetadata1.toString()).createOrOpen();

        final MapStoreIndexName indexName2 = indexName("table", "index2");
        final MapDBIndexMetadata indexMetadata2 = indexMetadata(indexName2,
            new MapStoreTableKey.Builder().setHash("hashField", String.class).build());
        mapDB.treeMap(indexMetadata2.toString()).createOrOpen();

        final Set<MapDBIndexMetadata> indexMetadatas = findIndexMetadata(mapDB, "table");
        assertThat(indexMetadatas, hasSize(2));
    }
}
