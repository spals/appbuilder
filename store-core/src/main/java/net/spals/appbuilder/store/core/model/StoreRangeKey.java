package net.spals.appbuilder.store.core.model;

/**
 * @author tkral
 */
public interface StoreRangeKey<C extends Comparable<C>> {

   StoreOperator getOperator();

   C getValue();
}
