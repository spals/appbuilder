package net.spals.appbuilder.graph.model;

import com.google.common.base.CharMatcher;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import net.spals.appbuilder.annotations.config.ApplicationName;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Type;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link ServiceGraphVertex}.
 *
 * @author tkral
 */
public class ServiceGraphVertexTest {

    @DataProvider
    Object[][] equalsProvider() {
        return new Object[][] {
            // Case: Non-Vertex objects
            {ServiceGraphVertex.newVertex(Key.get(String.class), ""), null, false},
            {ServiceGraphVertex.newVertex(Key.get(String.class), ""), new Object(), false},
            // Case: Same key types, same instances
            {ServiceGraphVertex.newVertex(Key.get(String.class), ""),
                ServiceGraphVertex.newVertex(Key.get(String.class), ""), true},
            // Case: Different instances
            {ServiceGraphVertex.newVertex(Key.get(String.class), ""),
                ServiceGraphVertex.newVertex(Key.get(String.class), "0"), false},
            // Case: Different key types
            {ServiceGraphVertex.newVertex(Key.get(String.class), ""),
                ServiceGraphVertex.newVertex(Key.get(Integer.class), 0), false},
            // Case: Mismatched annotations
            {ServiceGraphVertex.newVertex(Key.get(String.class, ApplicationName.class), ""),
                ServiceGraphVertex.newVertex(Key.get(String.class), ""), false},
            // Case: Same key types, same annotations, same instances
            {ServiceGraphVertex.newVertex(Key.get(String.class, ApplicationName.class), ""),
                ServiceGraphVertex.newVertex(Key.get(String.class, ApplicationName.class), ""), true},
        };
    }

    @Test(dataProvider = "equalsProvider")
    public void testEquals(final ServiceGraphVertex vertex, final Object obj, final boolean expectedResult) {
        assertThat(vertex.equals(obj), is(expectedResult));
    }

    @Test(dataProvider = "equalsProvider")
    public void testHashCode(final ServiceGraphVertex vertex, final Object obj, final boolean expectedResult) {
        if (obj != null && obj instanceof ServiceGraphVertex) {
            final Matcher<Integer> hashCodeMatcher = expectedResult ? is(vertex.hashCode()) : not(vertex.hashCode());
            assertThat(obj.hashCode(), hashCodeMatcher);
        }
    }

    @Test
    public void testInHashSet() {
        final HashSet<ServiceGraphVertex> vertexSet = new HashSet<>();
        vertexSet.add(ServiceGraphVertex.newVertex(Key.get(String.class), ""));
        vertexSet.add(ServiceGraphVertex.newVertex(Key.get(String.class), ""));

        assertThat(vertexSet, hasSize(1));
    }

    @DataProvider
    Object[][] simpleTypeNameProvider() {
        return new Object[][] {
            {String.class, "", "String"},
            {String[].class, new String[0], "String[]"},
            {ServiceGraphVertexTest.class, new ServiceGraphVertexTest(), "ServiceGraphVertexTest"},
            {ServiceGraphVertexTest[].class, new ServiceGraphVertexTest[0], "ServiceGraphVertexTest[]"},
            {CharMatcher.class, CharMatcher.any(), "com.google.common.base.CharMatcher"},
        };
    }

    @Test(dataProvider = "simpleTypeNameProvider")
    public void testSimpleTypeName(final Class<? extends Object> simpleType,
                                   final Object serviceInstance,
                                   final String expectedName) {
        final TypeLiteral<? extends Object> typeLiteral = TypeLiteral.get(simpleType);
        final Key<Object> key = (Key<Object>) Key.get(typeLiteral);
        final ServiceGraphVertex vertex = ServiceGraphVertex.newVertex(key, serviceInstance);
        assertThat(vertex.simpleTypeName(simpleType), is(expectedName));
    }

    @DataProvider
    Object[][] genericTypeNameProvider() {
        return new Object[][] {
            {new TypeLiteral<Set<String>>(){},
                Collections.<String>emptySet(),
                "Set<String>"},
            {new TypeLiteral<Set<ServiceGraphVertexTest>>(){},
                Collections.<ServiceGraphVertexTest>emptySet(),
                "Set<ServiceGraphVertexTest>"},
            {new TypeLiteral<Map<String, String>>(){},
                Collections.<String, String>emptyMap(),
                "Map<String, String>"},
            {new TypeLiteral<Map.Entry<String, String>>(){},
                new AbstractMap.SimpleEntry<String, String>("a", "b"),
                "Map.Entry<String, String>"},
            {new TypeLiteral<Set<Map.Entry<String, String>>>(){},
                Collections.singleton(new AbstractMap.SimpleEntry<String, String>("a", "b")),
                "Set<Map.Entry<String, String>>"},
            {new TypeLiteral<Provider<String>>(){},
                new Provider<String>() {
                    @Override
                    public String get() {
                        return "";
                    }
                },
                "Provider<String>"},
        };
    }

    @Test(dataProvider = "genericTypeNameProvider")
    public void testGenericTypeName(final TypeLiteral<Object> typeLiteral,
                                    final Object serviceInstance,
                                    final String expectedName) {
        final ServiceGraphVertex vertex = ServiceGraphVertex.newVertex(Key.get(typeLiteral), serviceInstance);
        assertThat(vertex.genericTypeName(typeLiteral), is(expectedName));
    }

    @DataProvider
    Object[][] typeLiteralNameProvider() {
        return new Object[][] {
            {TypeLiteral.get(String.class), "", "String"},
            {new TypeLiteral<Set<Map.Entry<String, String>>>(){},
                Collections.singleton(new AbstractMap.SimpleEntry<String, String>("a", "b")),
                "Set<Map.Entry<String, String>>"},
        };
    }

    @Test(dataProvider = "typeLiteralNameProvider")
    public void testTypeLiteralName(final TypeLiteral<Object> typeLiteral,
                                    final Object serviceInstance,
                                    final String expectedName) {
        final ServiceGraphVertex vertex = ServiceGraphVertex.newVertex(Key.get(typeLiteral), serviceInstance);
        assertThat(vertex.typeLiteralName(typeLiteral), is(expectedName));
    }
}
