package net.spals.appbuilder.mapstore.core.migration;

import net.spals.appbuilder.mapstore.core.MapStore;

/**
 * @author tkral
 */
@FunctionalInterface
public interface MapStoreMigration<C> {

    void migrate(C nativeClient, MapStore mapStore);
}
