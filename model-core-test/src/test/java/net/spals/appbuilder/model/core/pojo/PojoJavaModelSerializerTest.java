package net.spals.appbuilder.model.core.pojo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
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

        final byte[] bytes = modelSerializer.serialize(modelObject);
        assertThat(bytes, is(notNullValue()));
        assertThat(bytes, not((Matcher)emptyArray()));

        final Object deserializedModelObject = modelSerializer.deserialize(bytes);
        assertThat(deserializedModelObject, instanceOf(modelObject.getClass()));
    }

    @Test(dataProvider = "basicModelProvider")
    public void testBasicModelJson(final Object modelObject) throws IOException {
        final PojoModelSerializer modelSerializer = new PojoModelSerializer();

        final String json = modelSerializer.jsonSerialize(modelObject);
        assertThat(json, is(notNullValue()));

        final Object deserializedModelObject = modelSerializer.jsonDeserialize(json);
        assertThat(deserializedModelObject, instanceOf(modelObject.getClass()));
    }

    @DataProvider
    Object[][] modelEqualityProvider() {
        return new Object[][] {
            {AutoValueClass.create("MyString")},
            {new BuilderClass.Builder().setStringValue("MyString").build()},
            {ImmutableList.of("value")},
            {ImmutableList.of("value1", "value2")},
            {ImmutableMap.of("key", "value")},
            {ImmutableMap.of("key1", "value1", "key2", "value2")},
        };
    }

    @Test(dataProvider = "modelEqualityProvider")
    public void testModelEquality(final Object modelObject) {
        final PojoModelSerializer modelSerializer = new PojoModelSerializer();

        final byte[] bytes = modelSerializer.serialize(modelObject);
        assertThat(bytes, is(notNullValue()));
        assertThat(bytes, not((Matcher)emptyArray()));

        final Object deserializedModelObject = modelSerializer.deserialize(bytes);
        assertThat(deserializedModelObject, is(modelObject));
    }

    // TODO: Replace with modelEqualityProvider
    @DataProvider
    Object[][] modelEqualityJsonProvider() {
        return new Object[][] {
            // TODO: Fix AutoValue for Json
//            {AutoValueClass.create("MyString")},
            {new BuilderClass.Builder().setStringValue("MyString").build()},
            // TODO: Fix single value ImmutableList for Json
//            {ImmutableList.of("value")},
            {ImmutableList.of("value1", "value2")},
            // TODO: Fix single value ImmutableMap for Json
//            {ImmutableMap.of("key", "value")},
            // TODO: Fix multi value ImmutableMap for Json
//            {ImmutableMap.of("key1", "value1", "key2", "value2")},
        };
    }

    @Test(dataProvider = "modelEqualityJsonProvider")
    public void testModelEqualityJson(final Object modelObject) throws IOException {
        final PojoModelSerializer modelSerializer = new PojoModelSerializer();

        final String json = modelSerializer.jsonSerialize(modelObject);
        assertThat(json, is(notNullValue()));

        final Object deserializedModelObject = modelSerializer.jsonDeserialize(json);
        assertThat(deserializedModelObject, is(modelObject));
    }

    private static class SimpleClass { }

    private static class SerializableClass implements Serializable {
        private static final long serialVersionUID = -1L;
    }
}
