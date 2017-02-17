package net.spals.appbuilder.mapstore.core.model;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Standard;

/**
 * @author tkral
 */
@AutoValue
public abstract class ZeroValueMapRangeKey implements MapRangeKey<String> {

    public static MapRangeKey<String> all() {
        return new AutoValue_ZeroValueMapRangeKey(Standard.ALL);
    }

    public static MapRangeKey<String> none() {
        return new AutoValue_ZeroValueMapRangeKey(Standard.NONE);
    }

    @Override
    public abstract MapRangeOperator getOperator();

    @Override
    public String getValue() {
        return null;
    }
}
