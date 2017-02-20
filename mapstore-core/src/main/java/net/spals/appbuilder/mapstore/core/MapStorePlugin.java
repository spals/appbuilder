package net.spals.appbuilder.mapstore.core;

import net.spals.appbuilder.mapstore.core.model.MapStoreKey;

import java.util.Map;
import java.util.function.Predicate;

/**
 * @author tkral
 */
public interface MapStorePlugin extends MapStore {

    default Predicate<Map.Entry> isNullOrEmptyEntry() {
        return entry -> entry.getValue() == null || "".equals(entry.getValue());
    }

    default void stripKey(final MapStoreKey key, final Map<String, Object> payload) {
        // Strip out the key values from the payload (if they exist)
        payload.remove(key.getHashField());
        key.getRangeField().ifPresent(rangeField -> payload.remove(rangeField));
    }
}
