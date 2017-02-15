package net.spals.appbuilder.store.core.model;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.store.core.model.StoreOperator.Standard;

/**
 * @author tkral
 */
@AutoValue
public abstract class ZeroValueRangeKey implements StoreRangeKey<String> {

    public static StoreRangeKey<String> all() {
        return new AutoValue_ZeroValueRangeKey(Standard.ALL);
    }

    public static StoreRangeKey<String> none() {
        return new AutoValue_ZeroValueRangeKey(Standard.NONE);
    }

    @Override
    public abstract StoreOperator getOperator();

    @Override
    public String getValue() {
        return null;
    }
}
