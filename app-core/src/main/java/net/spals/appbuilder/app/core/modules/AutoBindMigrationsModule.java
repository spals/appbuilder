package net.spals.appbuilder.app.core.modules;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.*;
import com.google.inject.multibindings.MapBinder;
import net.spals.appbuilder.annotations.migration.AutoBindMigration;
import net.spals.appbuilder.annotations.service.*;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author tkral
 */
public class AutoBindMigrationsModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindMigrationsModule.class);

    private final Reflections serviceScan;

    public AutoBindMigrationsModule(final Reflections serviceScan) {
        this.serviceScan = serviceScan;
    }

    @Override
    protected void configure() {
        autoBindMigrations(binder());
    }

    @VisibleForTesting
    void autoBindMigrations(final Binder binder) {
        final Set<Class<?>> migrationClasses = serviceScan.getTypesAnnotatedWith(AutoBindMigration.class);
//        validateSingletons(mapClasses, AutoBindInMap.class);

        migrationClasses.stream()
            .forEach(migrationClazz -> {
                final AutoBindMigration autoBindMigration = migrationClazz.getAnnotation(AutoBindMigration.class);
                checkState(autoBindMigration.index() > 0,
                        "@AutoBindMigration.index must be greater than 0");
                LOGGER.info("Binding @AutoBindMigration[{}]: {}", autoBindMigration.index(),
                        autoBindMigration.description());

                final MapBinder migrationBinder = MapBinder.newMapBinder(binder, TypeLiteral.get(Integer.class),
                        TypeLiteral.get(migrationClazz.getClass()));
                migrationBinder.addBinding(autoBindMigration.index())
                        .to(migrationClazz).asEagerSingleton();
            });
    }
}
