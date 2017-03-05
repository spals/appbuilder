package net.spals.appbuilder.mapstore.core.migration;

import net.spals.appbuilder.mapstore.core.MapStore;

/**
 * @author tkral
 */
@FunctionalInterface
public interface MapStoreMigration {

    void migrate(MapStore mapStore);
}
