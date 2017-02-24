package net.spals.appbuilder.annotations.migration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a migration type that is to be auto-run
 * during application startup.
 *
 * @author tkral
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoBindMigration {

    /**
     * The unique index of the migration.
     *
     * This should consist of ever-increasing
     * integers.
     */
    int index();

    /**
     * A description of the changes created
     * by the migration.
     */
    String description();
}
