package net.spals.appbuilder.store.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = StoreKey.Builder.class)
public interface StoreKey {

    String getHashField();

    String getHashValue();

    Optional<String> getRangeField();

    Optional<Object> getRangeValue();

    class Builder extends StoreKey_Builder {

        @Override
        public Builder setRangeValue(final Object rangeValue) {
            checkArgument((rangeValue instanceof Boolean)
                    || (rangeValue instanceof Number)
                    || (rangeValue instanceof String),
                    "Illegal range value type. Must be Boolean, Number, or String. But is %s", rangeValue.getClass());

            return super.setRangeValue(rangeValue);
        }

        @Override
        public StoreKey build() {
            checkState(getRangeField().isPresent() == getRangeValue().isPresent(),
                    "Either both range key and range value must be set or neither can be set");
            return super.build();
        }
    }
}
