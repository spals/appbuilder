package net.spals.appbuilder.mapstore.core.migration;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import net.spals.appbuilder.annotations.config.ApplicationName;
import net.spals.appbuilder.annotations.migration.AutoBindMigration;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.annotations.MapStoreNativeClient;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.spals.appbuilder.mapstore.core.model.SingleValueMapRangeKey.equalTo;
import static net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.max;

/**
 * @author tkral
 */
@AutoBindSingleton(baseClass = MapStoreMigrationRunner.class)
class DefaultMapStoreMigrationRunner<C> implements MapStoreMigrationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMapStoreMigrationRunner.class);

    private static final String MIGRATIONS_TABLE_NAME = "migrations";

    private static final String APPLICATION_NAME_KEY = "applicationName";
    private static final String DESCRIPTION_KEY = "description";
    private static final String MIGRATION_INDEX = "migrationIndex";

    private final String applicationName;
    private final C nativeClient;
    private final MapStore mapStore;
    private final Map<Integer, MapStoreMigration<C>> storeMigrations;

    @Inject
    DefaultMapStoreMigrationRunner(@ApplicationName final String applicationName,
                                   @MapStoreNativeClient final C nativeClient,
                                   final MapStore mapStore,
                                   final Map<Integer, MapStoreMigration<C>> storeMigrations) {
        this.applicationName = applicationName;
        this.nativeClient = nativeClient;
        this.mapStore = mapStore;
        this.storeMigrations = storeMigrations;
    }

    @Override
    public void runMigrations() {
        // 1. Create a migrations table within the map store
        createMigrationsTable();
        // 2. Lookup the index of the last migration run
        final int lastMigrationIndex = lookupLastMigrationIndex();
        LOGGER.info("Last migration found for {} is {}", applicationName, lastMigrationIndex);

        // 3. Filter any bound migrations by index, finding those that come after
        // the last migration run
        final List<MapStoreMigration<C>> migrationsToRun = storeMigrations.entrySet().stream()
                .filter(entry -> entry.getKey() > lastMigrationIndex)
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        // 4. Run all pending migrations
        migrationsToRun.forEach(migration -> {
            final AutoBindMigration autoBindMigration = migration.getClass().getAnnotation(AutoBindMigration.class);
            final MapStoreKey migrationKey = new MapStoreKey.Builder()
                    .setHash(APPLICATION_NAME_KEY, applicationName)
                    .setRange(MIGRATION_INDEX, equalTo(autoBindMigration.index()))
                    .build();

            LOGGER.info("Running migration {} for {}: {}", new Object[] {autoBindMigration.index(),
                    applicationName, autoBindMigration.description()});
            migration.migrate(nativeClient, mapStore);
            mapStore.putItem(MIGRATIONS_TABLE_NAME, migrationKey,
                    ImmutableMap.of(DESCRIPTION_KEY, autoBindMigration.description()));
        });
    }

    void createMigrationsTable() {
        final MapStoreTableKey migrationsTableKey = new MapStoreTableKey.Builder()
                .setHash(APPLICATION_NAME_KEY, String.class)
                .setRange(MIGRATION_INDEX, Integer.class)
                .build();
        final boolean createTableResult = mapStore.createTable(MIGRATIONS_TABLE_NAME, migrationsTableKey);
        if (!createTableResult) {
            throw new IllegalStateException("Could not create migrations table.");
        }
    }

    int lookupLastMigrationIndex() {
        final MapStoreKey lastMigrationKey = new MapStoreKey.Builder()
                .setHash(APPLICATION_NAME_KEY, applicationName)
                .setRange(MIGRATION_INDEX, max())
                .build();
        return mapStore.getItem(MIGRATIONS_TABLE_NAME, lastMigrationKey)
                .map(migration -> (Integer) migration.get(MIGRATION_INDEX)).orElse(-1);
    }
}
