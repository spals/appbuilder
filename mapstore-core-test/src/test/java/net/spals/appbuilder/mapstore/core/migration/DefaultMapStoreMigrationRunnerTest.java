package net.spals.appbuilder.mapstore.core.migration;

import com.google.common.collect.ImmutableMap;
import net.spals.appbuilder.annotations.migration.AutoBindMigration;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.googlecode.catchexception.CatchException.verifyException;
import static net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.equalTo;
import static net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.max;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.same;

/**
 * Unit tests for {@link DefaultMapStoreMigrationRunner}
 *
 * @author tkral
 */
public class DefaultMapStoreMigrationRunnerTest {

    @Test
    public void testCreateMigrationsTable() {
        final MapStore mapStore = mock(MapStore.class);
        when(mapStore.createTable(anyString(), any(MapStoreTableKey.class))).thenReturn(true);

        final DefaultMapStoreMigrationRunner migrationRunner =
            new DefaultMapStoreMigrationRunner("myApplicationName", mapStore, Collections.emptyMap());

        migrationRunner.createMigrationsTable();

        final MapStoreTableKey expectedTableKey = new MapStoreTableKey.Builder()
            .setHash("applicationName", String.class)
            .setRange("migrationIndex", Integer.class)
            .build();
        verify(mapStore).createTable(eq("migrations"), eq(expectedTableKey));
    }

    @Test
    public void testCreateMigrationsTableError() {
        final MapStore mapStore = mock(MapStore.class);
        when(mapStore.createTable(anyString(), any(MapStoreTableKey.class))).thenReturn(false);

        final DefaultMapStoreMigrationRunner migrationRunner =
            new DefaultMapStoreMigrationRunner("myApplicationName", mapStore, Collections.emptyMap());

        verifyException(() -> migrationRunner.createMigrationsTable(), IllegalStateException.class);
    }

    @Test
    public void testLookupLastMigrationIndex() {
        final MapStore mapStore = mock(MapStore.class);
        final DefaultMapStoreMigrationRunner migrationRunner =
            new DefaultMapStoreMigrationRunner("myApplicationName", mapStore, Collections.emptyMap());

        final int migrationIndex = migrationRunner.lookupLastMigrationIndex();

        final MapStoreKey expectedKey = new MapStoreKey.Builder()
            .setHash("applicationName", "myApplicationName")
            .setRange("migrationIndex", max())
            .build();
        verify(mapStore).getItem(eq("migrations"), eq(expectedKey));
        // We do not provide a return value in the mock so it will return null
        // and we'd expect that to result in a -1 index
        assertThat(migrationIndex, is(-1));
    }

    @Test
    public void testLookupLastMigrationIndexParse() {
        final MapStore mapStore = mock(MapStore.class);
        when(mapStore.getItem(anyString(), any(MapStoreKey.class)))
            .thenReturn(Optional.of(ImmutableMap.of("applicationName", "myApplicationName",
                "migrationIndex", 0,
                "description", "myDesc")));

        final DefaultMapStoreMigrationRunner migrationRunner =
            new DefaultMapStoreMigrationRunner("myApplicationName", mapStore, Collections.emptyMap());

        final int migrationIndex = migrationRunner.lookupLastMigrationIndex();
        assertThat(migrationIndex, is(0));
    }

    @Test
    public void testRunMigrations() {
        final MapStore mapStore = mock(MapStore.class);
        when(mapStore.createTable(anyString(), any(MapStoreTableKey.class))).thenReturn(true);
        when(mapStore.getItem(anyString(), any(MapStoreKey.class)))
            .thenReturn(Optional.of(ImmutableMap.of("applicationName", "myApplicationName",
                "migrationIndex", 0,
                "description", "myDesc")));

        final MapStoreMigration migration0 = Mockito.spy(new NoopMigration0());
        final MapStoreMigration migration1 = Mockito.spy(new NoopMigration1());
        final MapStoreMigration migration2 = Mockito.spy(new NoopMigration2());
        final Map<Integer, MapStoreMigration> storeMigrations =
            ImmutableMap.of(0, migration0, 1, migration1, 2, migration2);

        final DefaultMapStoreMigrationRunner migrationRunner =
            new DefaultMapStoreMigrationRunner("myApplicationName", mapStore, storeMigrations);

        migrationRunner.runMigrations();
        // Migration 0 is skipped because it has already been run (migrationIndex is 0)
        verify(migration0, never()).migrate(any(MapStore.class));
        verify(migration1).migrate(same(mapStore));
        verify(migration2).migrate(same(mapStore));

        verify(mapStore).putItem(eq("migrations"), eq(expectedMigrationsKey("myApplicationName", 1)),
            eq(Collections.singletonMap("description", "Migration 1")));
        verify(mapStore).putItem(eq("migrations"), eq(expectedMigrationsKey("myApplicationName", 2)),
            eq(Collections.singletonMap("description", "Migration 2")));
    }

    private MapStoreKey expectedMigrationsKey(final String applicationName, final int index) {
        return new MapStoreKey.Builder()
            .setHash("applicationName", applicationName)
            .setRange("migrationIndex", equalTo(index))
            .build();
    }

    @AutoBindMigration(index = 0, description = "Migration 0")
    private static class NoopMigration0 implements MapStoreMigration {
        @Override
        public void migrate(final MapStore mapStore) {  }
    }

    @AutoBindMigration(index = 1, description = "Migration 1")
    private static class NoopMigration1 implements MapStoreMigration {
        @Override
        public void migrate(final MapStore mapStore) {  }
    }

    @AutoBindMigration(index = 2, description = "Migration 2")
    private static class NoopMigration2 implements MapStoreMigration {
        @Override
        public void migrate(final MapStore mapStore) {  }
    }
}
