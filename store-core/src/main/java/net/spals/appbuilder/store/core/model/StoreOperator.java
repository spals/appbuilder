package net.spals.appbuilder.store.core.model;

/**
 * @author tkral
 */
public interface StoreOperator {

    enum Standard implements StoreOperator {
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
