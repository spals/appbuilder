package net.spals.appbuilder.mapstore.core.model;

import org.testng.annotations.Test;

import static net.spals.appbuilder.mapstore.core.model.MapStoreIndexName.fromString;
import static net.spals.appbuilder.mapstore.core.model.MapStoreIndexName.indexName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link MapStoreIndexName}.
 *
 * @author tkral
 */
public class MapStoreIndexNameTest {

    @Test
    public void testToString() {
        final MapStoreIndexName indexName = indexName("table", "index");
        assertThat(indexName.toString(), is("table.index"));
    }

    @Test
    public void testFromString() {
        assertThat(fromString("table.index"), is(indexName("table", "index")));
    }
}
