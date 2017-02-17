package net.spals.appbuilder.mapstore.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.spals.appbuilder.mapstore.core.model.MapRangeOperator.Standard;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static net.spals.appbuilder.mapstore.core.model.ZeroValueMapRangeKey.none;

/**
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = MapStoreKey.Builder.class)
public interface MapStoreKey {

    String getHashField();

    String getHashValue();

    Optional<String> getRangeField();

    MapRangeKey getRangeKey();

    class Builder extends MapStoreKey_Builder {

        public Builder() {
            setRangeKey(none());
        }

        public Builder setHash(final String hashField, final String hashValue) {
            setHashField(hashField);
            return setHashValue(hashValue);
        }

        public Builder setRange(final String rangeField, final MapRangeKey rangeKey) {
            setRangeField(rangeField);
            return setRangeKey(rangeKey);
        }

        @Override
        public MapStoreKey build() {
            checkState(getRangeKey().getOperator() == Standard.NONE || getRangeField().isPresent());
            return super.build();
        }
    }
}
