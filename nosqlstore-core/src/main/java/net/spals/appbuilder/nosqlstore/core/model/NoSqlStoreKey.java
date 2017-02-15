package net.spals.appbuilder.nosqlstore.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.spals.appbuilder.nosqlstore.core.model.NoSqlStoreOperator.Standard;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static net.spals.appbuilder.nosqlstore.core.model.ZeroValueRangeKey.none;

/**
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = NoSqlStoreKey.Builder.class)
public interface NoSqlStoreKey {

    String getHashField();

    String getHashValue();

    Optional<String> getRangeField();

    NoSqlStoreRangeKey getRangeKey();

    class Builder extends NoSqlStoreKey_Builder {

        public Builder() {
            setRangeKey(none());
        }

        public Builder setHash(final String hashField, final String hashValue) {
            setHashField(hashField);
            return setHashValue(hashValue);
        }

        public Builder setRange(final String rangeField, final NoSqlStoreRangeKey rangeKey) {
            setRangeField(rangeField);
            return setRangeKey(rangeKey);
        }

        @Override
        public NoSqlStoreKey build() {
            checkState(getRangeKey().getOperator() == Standard.NONE || getRangeField().isPresent());
            return super.build();
        }
    }
}
