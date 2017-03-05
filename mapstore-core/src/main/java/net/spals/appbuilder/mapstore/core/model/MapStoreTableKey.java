package net.spals.appbuilder.mapstore.core.model;

import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author tkral
 */
@FreeBuilder
public interface MapStoreTableKey {

    String getHashField();

    Class<?> getHashFieldType();

    Optional<String> getRangeField();

    Optional<Class<? extends Comparable>> getRangeFieldType();

    class Builder extends MapStoreTableKey_Builder {

        public Builder setHash(final String hashField, final Class<?> hashFieldType) {
            setHashField(hashField);
            return setHashFieldType(hashFieldType);
        }

        public Builder setRange(final String rangeField, final Class<? extends Comparable> rangeFieldType) {
            setRangeField(rangeField);
            return setRangeFieldType(rangeFieldType);
        }

        @Override
        public MapStoreTableKey build() {
            checkState(getRangeField().isPresent() == getRangeFieldType().isPresent());
            return super.build();
        }
    }
}
