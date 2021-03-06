package net.spals.appbuilder.mapstore.core.model;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Standard;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.SyntacticSugar;

/**
 * @author tkral
 */
@AutoValue
public abstract class ZeroValueMapRangeKey implements MapRangeKey<String> {

    public static MapRangeKey<String> all() {
        return new AutoValue_ZeroValueMapRangeKey(Standard.ALL);
    }

    public static MapRangeKey<String> max() {
        return new AutoValue_ZeroValueMapRangeKey(SyntacticSugar.MAX);
    }

    public static MapRangeKey<String> min() {
        return new AutoValue_ZeroValueMapRangeKey(SyntacticSugar.MIN);
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
