package net.spals.appbuilder.message.core.formatter.serializable;

import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.Serializable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link SerializableMessageFormatter}
 *
 * @author tkral
 */
public class SerializableMessageFormatterTest {

    @DataProvider
    Object[][] basicPayloadProvider() {
        return new Object[][] {
                {new SimpleClass()},
                {new SerializableClass()},
        };
    }

    @Test(dataProvider = "basicPayloadProvider")
    public void testBasicPayload(final Object payload) {
        final SerializableMessageFormatter messsageFormatter = new SerializableMessageFormatter();

        final byte[] serializedPayload = messsageFormatter.serializePayload(payload);
        assertThat(serializedPayload, is(notNullValue()));
        assertThat(serializedPayload, not((Matcher)emptyArray()));

        final Object deserializedPayload = messsageFormatter.deserializePayload(serializedPayload);
        assertThat(deserializedPayload, instanceOf(payload.getClass()));
    }

    @DataProvider
    Object[][] payloadEqualityProvider() {
        return new Object[][] {
                {AutoValueClass.create("MyString")},
                {new BuilderClass.Builder().setStringValue("MyString").build()},
        };
    }

    @Test(dataProvider = "payloadEqualityProvider")
    public void testPayloadEquality(final Object payload) {
        final SerializableMessageFormatter messsageFormatter = new SerializableMessageFormatter();

        final byte[] serializedPayload = messsageFormatter.serializePayload(payload);
        assertThat(serializedPayload, is(notNullValue()));
        assertThat(serializedPayload, not((Matcher)emptyArray()));

        final Object deserializedPayload = messsageFormatter.deserializePayload(serializedPayload);
        assertThat(deserializedPayload, is(payload));
    }

    private static class SimpleClass { }

    private static class SerializableClass implements Serializable {
        private static final long serialVersionUID = -1L;
    }
}
