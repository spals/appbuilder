package net.spals.appbuilder.store.core.model;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.store.core.model.StoreOperator.Standard;

/**
 * @author tkral
 */
@AutoValue
public abstract class SingleValueRangeKey<C extends Comparable<C>> implements StoreRangeKey<C> {

    public static <C extends Comparable<C>> StoreRangeKey<C> equalTo(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.EQUAL_TO, value);
    }

    public static <C extends Comparable<C>> StoreRangeKey<C> greaterThan(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.GREATER_THAN, value);
    }

    public static <C extends Comparable<C>> StoreRangeKey<C> greaterThanOrEqualTo(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.GREATER_THAN_OR_EQUAL_TO, value);
    }

    public static <C extends Comparable<C>> StoreRangeKey<C> lessThan(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.LESS_THAN, value);
    }

    public static <C extends Comparable<C>> StoreRangeKey<C> lessThanOrEqualTo(final C value) {
        return new AutoValue_SingleValueRangeKey<>(Standard.LESS_THAN_OR_EQUAL_TO, value);
    }

    @Override
    public abstract StoreOperator getOperator();

    @Override
    public abstract C getValue();
}
