package net.spals.appbuilder.store.core.model;

import com.google.auto.value.AutoValue;
import net.spals.appbuilder.store.core.model.StoreOperator.Standard;
import net.spals.appbuilder.store.core.model.TwoValueRangeKey.TwoValueHolder;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author tkral
 */
@AutoValue
public abstract class TwoValueRangeKey<C extends Comparable<C>> implements StoreRangeKey<TwoValueHolder<C>> {

    public static <C extends Comparable<C>> StoreRangeKey<TwoValueHolder<C>> between(final C value1, final C value2) {
        checkArgument(value1.compareTo(value2) <= 0, "Between values must be in ascending order");
        return new AutoValue_TwoValueRangeKey<>(Standard.BETWEEN, TwoValueHolder.create(value1, value2));
    }

    @Override
    public abstract StoreOperator getOperator();

    @Override
    public abstract TwoValueHolder<C> getValue();

    @AutoValue
    public static abstract class TwoValueHolder<C extends Comparable<C>> implements Comparable<TwoValueHolder<C>> {

        private static <C extends Comparable<C>> TwoValueHolder<C> create(final C value1, final C value2) {
            return new AutoValue_TwoValueRangeKey_TwoValueHolder<>(value1, value2);
        }

        public abstract C getValue1();

        public abstract C getValue2();

        @Override
        public int compareTo(final TwoValueHolder<C> holder) {
            // We just need this to provide type-safety.
            // We'll never really compare TwoValueHolders.
            return 0;
        }
    }
}
