package net.spals.appbuilder.mapstore.core.migration;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.multibindings.MapBinder;
import net.spals.appbuilder.annotations.config.ServiceScan;
import net.spals.appbuilder.annotations.migration.AutoBindMigration;
import net.spals.appbuilder.annotations.service.AutoBindModule;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author tkral
 */
@AutoBindModule
class AutoBindMigrationsModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindMigrationsModule.class);

    private final Reflections serviceScan;

    @Inject
    public AutoBindMigrationsModule(@ServiceScan final Reflections serviceScan) {
        this.serviceScan = serviceScan;
    }

    @Override
    protected void configure() {
        autoBindMigrations(binder());
    }

    @VisibleForTesting
    void autoBindMigrations(final Binder binder) {
        final MapBinder migrationBinder = MapBinder.newMapBinder(binder, Integer.class, MapStoreMigration.class);
        final Set<Class<?>> migrationClasses = serviceScan.getTypesAnnotatedWith(AutoBindMigration.class);
        validateMigrations(migrationClasses);

        migrationClasses.stream()
            .forEach(migrationClazz -> {
                final AutoBindMigration autoBindMigration = migrationClazz.getAnnotation(AutoBindMigration.class);
                checkState(autoBindMigration.index() > 0,
                        "@AutoBindMigration.index must be greater than 0");
                LOGGER.info("Binding @AutoBindMigration[{}]: {}", autoBindMigration.index(),
                        autoBindMigration.description());

                migrationBinder.addBinding(autoBindMigration.index())
                        .to(migrationClazz).asEagerSingleton();
            });
    }

    @VisibleForTesting
    void validateMigrations(final Set<Class<?>> migrationClasses) {
        final Set<Class<?>> invalidMigrations = migrationClasses.stream()
                .filter(migrationClazz -> migrationClazz.isInterface()
                        || !MapStoreMigration.class.isAssignableFrom(migrationClazz))
                .collect(Collectors.toSet());
        checkState(invalidMigrations.isEmpty(),
                "@AutoBindMigration can only annotate MapStoreMigration classes: %s", invalidMigrations);
    }
}
