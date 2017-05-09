package net.spals.appbuilder.model.core.pojo;

import com.google.auto.value.AutoValue;

/**
 * AutoValue test class for SerializableMesssageFormatterTest.
 *
 * This is separated so that generated classes can be created.
 *
 * @author tkral
 */
@AutoValue
abstract class AutoValueClass {

    static AutoValueClass create(final String stringValue) {
        return new AutoValue_AutoValueClass(stringValue);
    }

    abstract String getStringValue();
}
