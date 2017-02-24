package net.spals.appbuilder.mapstore.core;

/**
 * @author tkral
 */
@FunctionalInterface
public interface MapStoreMigration<C> {

    void migrate(C nativeClient, MapStore mapStore);
}
