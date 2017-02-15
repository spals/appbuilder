package net.spals.appbuilder.nosqlstore.core.model;

/**
 * @author tkral
 */
public interface NoSqlStoreRangeKey<C extends Comparable<C>> {

   NoSqlStoreOperator getOperator();

   C getValue();
}
