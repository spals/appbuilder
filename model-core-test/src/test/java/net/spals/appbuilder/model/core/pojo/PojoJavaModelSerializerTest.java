package net.spals.appbuilder.model.core.pojo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.Serializable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link PojoModelSerializer}
 *
 * @author tkral
 */
public class PojoJavaModelSerializerTest {

    @DataProvider
    Object[][] basicModelProvider() {
        return new Object[][] {
                {new SimpleClass()},
                {new SerializableClass()},
        };
    }

    @Test(dataProvider = "basicModelProvider")
    public void testBasicModel(final Object modelObject) {
        final PojoModelSerializer modelSerializer = new PojoModelSerializer();

        final byte[] serializedModelObject = modelSerializer.serialize(modelObject);
        assertThat(serializedModelObject, is(notNullValue()));
        assertThat(serializedModelObject, not((Matcher)emptyArray()));

        final Object deserializedModelObject = modelSerializer.deserialize(serializedModelObject);
        assertThat(deserializedModelObject, instanceOf(modelObject.getClass()));
    }

    @DataProvider
    Object[][] modelEqualityProvider() {
        return new Object[][] {
                {AutoValueClass.create("MyString")},
                {new BuilderClass.Builder().setStringValue("MyString").build()},
                {ImmutableList.of("value1", "value2")},
                {ImmutableMap.of("key", "value")},
        };
    }

    @Test(dataProvider = "modelEqualityProvider")
    public void testModelEquality(final Object modelObject) {
        final PojoModelSerializer modelSerializer = new PojoModelSerializer();

        final byte[] serializedModelObject = modelSerializer.serialize(modelObject);
        assertThat(serializedModelObject, is(notNullValue()));
        assertThat(serializedModelObject, not((Matcher)emptyArray()));

        final Object deserializedModelObject = modelSerializer.deserialize(serializedModelObject);
        assertThat(deserializedModelObject, is(modelObject));
    }

    private static class SimpleClass { }

    private static class SerializableClass implements Serializable {
        private static final long serialVersionUID = -1L;
    }
}
