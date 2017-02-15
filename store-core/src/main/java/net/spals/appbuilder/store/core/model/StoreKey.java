package net.spals.appbuilder.store.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.spals.appbuilder.store.core.model.StoreOperator.Standard;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static net.spals.appbuilder.store.core.model.ZeroValueRangeKey.none;

/**
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = StoreKey.Builder.class)
public interface StoreKey {

    String getHashField();

    String getHashValue();

    Optional<String> getRangeField();

    StoreRangeKey getRangeKey();

    class Builder extends StoreKey_Builder {

        public Builder() {
            setRangeKey(none());
        }

        public Builder setHash(final String hashField, final String hashValue) {
            setHashField(hashField);
            return setHashValue(hashValue);
        }

        public Builder setRange(final String rangeField, final StoreRangeKey rangeKey) {
            setRangeField(rangeField);
            return setRangeKey(rangeKey);
        }

        @Override
        public StoreKey build() {
            checkState(getRangeKey().getOperator() == Standard.NONE || getRangeField().isPresent());
            return super.build();
        }
    }
}
