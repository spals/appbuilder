package net.spals.appbuilder.store.core;

import net.spals.appbuilder.store.core.model.StoreKey;
import net.spals.appbuilder.store.core.model.StoreOperator;
import net.spals.appbuilder.store.core.model.StoreOperator.Standard;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author tkral
 */
public interface StorePlugin extends Store {

    default void checkKeyField(final String keyField, final Object keyValue, final Map<String, Object> payload) {
        checkArgument(!payload.containsKey(keyField) || keyValue.equals(payload.get(keyField)),
                "Mismatched key value (%s) and payload field value (%s)",
                keyValue, payload.get(keyField));
    }

    default void checkSingleItemKey(final StoreKey key) {
        if (key.getRangeField().isPresent()) {
            checkArgument(key.getRangeKey().getOperator() == Standard.EQUAL_TO,
                    "Illegal range operator found (%s). You must use EQUAL_TO with value", key.getRangeKey().getOperator());
        } else {
            checkArgument(key.getRangeKey().getOperator() == Standard.NONE,
                    "Illegal range operator found (%s). You must use NONE without value", key.getRangeKey().getOperator());
        }
    }

    default void checkWriteItem(final StoreKey key, final Map<String, Object> payload) {
        checkArgument(!payload.isEmpty(), "Cannot write item with empty payload");
        // Ensure that the hash value matches what's in the payload (or is absent)
        checkKeyField(key.getHashField(), key.getHashValue(), payload);
        // Ensure that the range value matches what's in the payload (or is absent)
        key.getRangeField().ifPresent(rangeField -> checkKeyField(rangeField, key.getRangeKey().getValue(), payload));
        // Write access only one item at a time
        checkSingleItemKey(key);
    }
}
