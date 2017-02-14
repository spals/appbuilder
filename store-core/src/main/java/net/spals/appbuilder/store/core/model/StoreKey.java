package net.spals.appbuilder.store.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Optional;

/**
 * @author tkral
 */
@FreeBuilder
@JsonDeserialize(builder = StoreKey.Builder.class)
public interface StoreKey {

    String getHashField();

    String getHashValue();

    Optional<String> getRangeField();

    Optional<String> getRangeValue();

    class Builder extends StoreKey_Builder {  }
}
