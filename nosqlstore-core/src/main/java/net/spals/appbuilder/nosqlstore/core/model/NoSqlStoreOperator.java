package net.spals.appbuilder.nosqlstore.core.model;

/**
 * @author tkral
 */
public interface NoSqlStoreOperator {

    enum Standard implements NoSqlStoreOperator {
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
