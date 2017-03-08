package net.spals.appbuilder.annotations;

import com.google.common.collect.ImmutableSet;
import net.spals.appbuilder.annotations.config.ApplicationName;
import net.spals.appbuilder.annotations.config.ServiceConfig;
import net.spals.appbuilder.annotations.config.ServiceScan;
import net.spals.appbuilder.annotations.migration.AutoBindMigration;
import net.spals.appbuilder.annotations.service.*;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * @author tkral
 */
public class AppBuilderAnnotations {

    private static final Set<Class<? extends Annotation>> ALL_CONFIG =
            ImmutableSet.<Class<? extends Annotation>>builder()
                    .add(ApplicationName.class, ServiceConfig.class, ServiceScan.class)
                    .build();

    private static final Set<Class<? extends Annotation>> ALL_MIGRATION =
            ImmutableSet.<Class<? extends Annotation>>builder()
                    .add(AutoBindMigration.class)
                    .build();

    private static final Set<Class<? extends Annotation>> ALL_SERVICE =
            ImmutableSet.<Class<? extends Annotation>>builder()
                    .add(AutoBindFactory.class, AutoBindInMap.class, AutoBindInSet.class,
                            AutoBindModule.class, AutoBindProvider.class, AutoBindSingleton.class)
                    .build();

    private AppBuilderAnnotations() { }

    public static Set<Class<? extends Annotation>> all() {
        return ImmutableSet.<Class<? extends Annotation>>builder()
                .addAll(ALL_CONFIG)
                .addAll(ALL_MIGRATION)
                .addAll(ALL_SERVICE)
                .build();
    }

    public static Set<Class<? extends Annotation>> allConfig() {
        return ALL_CONFIG;
    }

    public static Set<Class<? extends Annotation>> allMigration() {
        return ALL_MIGRATION;
    }

    public static Set<Class<? extends Annotation>> allService() {
        return ALL_SERVICE;
    }
}
