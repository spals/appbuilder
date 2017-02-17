package net.spals.appbuilder.mapstore.core.model;

/**
 * @author tkral
 */
public interface MapRangeOperator {

    enum Standard implements MapRangeOperator {
        ALL,
        BETWEEN,
        EQUAL_TO,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL_TO,
        LESS_THAN,
        LESS_THAN_OR_EQUAL_TO,
        NONE,
        ;
    }
}
