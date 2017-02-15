package net.spals.appbuilder.nosqlstore.core.model;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.nosqlstore.core.model.NoSqlStoreOperator.Standard;

/**
 * @author tkral
 */
@AutoValue
public abstract class ZeroValueRangeKey implements NoSqlStoreRangeKey<String> {

    public static NoSqlStoreRangeKey<String> all() {
        return new AutoValue_ZeroValueRangeKey(Standard.ALL);
    }

    public static NoSqlStoreRangeKey<String> none() {
        return new AutoValue_ZeroValueRangeKey(Standard.NONE);
    }

    @Override
    public abstract NoSqlStoreOperator getOperator();

    @Override
    public String getValue() {
        return null;
    }
}
