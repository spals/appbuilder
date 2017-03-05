package net.spals.appbuilder.mapstore.core.model;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Extended;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Standard;

/**
 * @author tkral
 */
@AutoValue
public abstract class SingleValueMapRangeKey<C extends Comparable<C>> implements MapRangeKey<C> {

    public static <C extends Comparable<C>> MapRangeKey<C> equalTo(final C value) {
        return new AutoValue_SingleValueMapRangeKey<>(Standard.EQUAL_TO, value);
    }

    public static <C extends Comparable<C>> MapRangeKey<C> greaterThan(final C value) {
        return new AutoValue_SingleValueMapRangeKey<>(Standard.GREATER_THAN, value);
    }

    public static <C extends Comparable<C>> MapRangeKey<C> greaterThanOrEqualTo(final C value) {
        return new AutoValue_SingleValueMapRangeKey<>(Standard.GREATER_THAN_OR_EQUAL_TO, value);
    }

    public static <C extends Comparable<C>> MapRangeKey<C> lessThan(final C value) {
        return new AutoValue_SingleValueMapRangeKey<>(Standard.LESS_THAN, value);
    }

    public static <C extends Comparable<C>> MapRangeKey<C> lessThanOrEqualTo(final C value) {
        return new AutoValue_SingleValueMapRangeKey<>(Standard.LESS_THAN_OR_EQUAL_TO, value);
    }

    public static MapRangeKey<String> like(final String value) {
        return new AutoValue_SingleValueMapRangeKey<>(Extended.LIKE, value);
    }

    public static MapRangeKey<String> startsWith(final String value) {
        return new AutoValue_SingleValueMapRangeKey<>(Extended.STARTS_WITH, value);
    }

    @Override
    public abstract MapRangeOperator getOperator();

    @Override
    public abstract C getValue();
}
