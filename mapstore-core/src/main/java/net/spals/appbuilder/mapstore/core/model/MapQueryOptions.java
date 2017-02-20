package net.spals.appbuilder.mapstore.core.model;

import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

/**
 * @author tkral
 */
@FreeBuilder
public interface MapQueryOptions {

    public static MapQueryOptions defaultOptions() {
        return new Builder().build();
    }

    Optional<Integer> getLimit();

    Order getOrder();

    enum Order {
        ASC,
        DESC,
        ;
    }

    class Builder extends MapQueryOptions_Builder {
        public Builder() {
            setOrder(Order.ASC);
        }
    }
}
