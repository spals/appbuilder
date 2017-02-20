package net.spals.appbuilder.mapstore.core;

import com.google.common.annotations.VisibleForTesting;
import net.spals.appbuilder.mapstore.core.model.MapStoreKey;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Standard;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author tkral
 */
public interface MapStorePlugin extends MapStore {

    @VisibleForTesting
    default Predicate<Map.Entry> isNullOrEmptyEntry() {
        return entry -> entry.getValue() == null || "".equals(entry.getValue());
    }
}
