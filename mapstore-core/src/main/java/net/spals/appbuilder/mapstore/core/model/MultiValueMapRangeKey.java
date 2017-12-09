package net.spals.appbuilder.mapstore.core.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Extended;
import net.spals.appbuilder.mapstore.core.model.MultiValueMapRangeKey.ListValueHolder;

import java.util.List;

/**
 * @author tkral
 */
@AutoValue
public abstract class MultiValueMapRangeKey<C extends Comparable<C>> implements MapRangeKey<ListValueHolder<C>> {

    public static <C extends Comparable<C>> MapRangeKey<ListValueHolder<C>> in(final C value) {
        final List<C> valueList = ImmutableList.of(value);
        return new AutoValue_MultiValueMapRangeKey<>(Extended.IN, ListValueHolder.create(valueList));
    }

    public static <C extends Comparable<C>> MapRangeKey<ListValueHolder<C>> in(final C value1, final C value2) {
        final List<C> valueList = ImmutableList.of(value1, value2);
        return new AutoValue_MultiValueMapRangeKey<>(Extended.IN, ListValueHolder.create(valueList));
    }

    public static <C extends Comparable<C>> MapRangeKey<ListValueHolder<C>> in(
        final C value1,
        final C value2,
        final C... values
    ) {
        final List<C> valueList = ImmutableList.<C>builder().add(value1).add(value2).add(values).build();
        return new AutoValue_MultiValueMapRangeKey<>(Extended.IN, ListValueHolder.create(valueList));
    }

    @Override
    public abstract MapRangeOperator getOperator();

    @Override
    public abstract ListValueHolder<C> getValue();

    @AutoValue
    public static abstract class ListValueHolder<C extends Comparable<C>> implements Comparable<ListValueHolder<C>> {

        private static <C extends Comparable<C>> ListValueHolder<C> create(final List<C> valueList) {
            return new AutoValue_MultiValueMapRangeKey_ListValueHolder<>(valueList);
        }

        public abstract List<C> getValues();

        @Override
        public int compareTo(final ListValueHolder<C> holder) {
            // We just need this to provide type-safety.
            // We'll never really compare ListValueHolders.
            return 0;
        }
    }
}
