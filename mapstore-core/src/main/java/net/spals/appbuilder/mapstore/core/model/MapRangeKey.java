package net.spals.appbuilder.mapstore.core.model;

/**
 * @author tkral
 */
public interface MapRangeKey<C extends Comparable<C>> {

   MapRangeOperator getOperator();

   C getValue();
}
