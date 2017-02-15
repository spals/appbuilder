package net.spals.appbuilder.store.core.model;

import com.google.auto.value.AutoValue;

/**
 * @author tkral
 */
@AutoValue
public abstract class TwoValueRangeKey<C extends Comparable<C>> implements StoreRangeKey<C> {

    @Override
    public abstract StoreOperator getOperator();


}
