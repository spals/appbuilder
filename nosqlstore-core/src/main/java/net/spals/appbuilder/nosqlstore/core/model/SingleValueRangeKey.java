package net.spals.appbuilder.nosqlstore.core.model;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.nosqlstore.core.model.NoSqlStoreOperator.Standard;

/**
 * @author tkral
 */
@AutoValue
public abstract class SingleValueRangeKey<C extends Comparable<C>> implements NoSqlStoreRangeKey<C> {

    public static <C extends Comparable<C>> NoSqlStoreRangeKey<C> equalTo(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.EQUAL_TO, value);
    }

    public static <C extends Comparable<C>> NoSqlStoreRangeKey<C> greaterThan(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.GREATER_THAN, value);
    }

    public static <C extends Comparable<C>> NoSqlStoreRangeKey<C> greaterThanOrEqualTo(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.GREATER_THAN_OR_EQUAL_TO, value);
    }

    public static <C extends Comparable<C>> NoSqlStoreRangeKey<C> lessThan(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.LESS_THAN, value);
    }

    public static <C extends Comparable<C>> NoSqlStoreRangeKey<C> lessThanOrEqualTo(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.LESS_THAN_OR_EQUAL_TO, value);
    }

    @Override
    public abstract NoSqlStoreOperator getOperator();

    @Override
    public abstract C getValue();
}
