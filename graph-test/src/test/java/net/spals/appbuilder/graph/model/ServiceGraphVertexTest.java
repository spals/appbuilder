package net.spals.appbuilder.graph.model;

import com.google.common.base.CharMatcher;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import net.spals.appbuilder.annotations.config.ApplicationName;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
            {String.class, "String"},
            {String[].class, "String[]"},
            {ServiceGraphVertexTest.class, "AsciiServiceGraphVertexTest"},
            {ServiceGraphVertexTest[].class, "AsciiServiceGraphVertexTest[]"},
            {CharMatcher.class, "com.google.common.base.CharMatcher"},
        };
    }

    @Test(dataProvider = "simpleTypeNameProvider")
    public void testSimpleTypeName(final Class<?> simpleType, final String expectedName) {
        final ServiceGraphVertex vertex = ServiceGraphVertex.newVertex(Key.get(simpleType), null);
        assertThat(vertex.simpleTypeName(simpleType), is(expectedName));
    }

//    @DataProvider
//    Object[][] genericTypeNameProvider() {
//        return new Object[][] {
//            {new TypeLiteral<Set<String>>(){}, "Set<String>"},
//            {new TypeLiteral<Set<ServiceGraphVertexTest>>(){}, "Set<AsciiServiceGraphVertexTest>"},
//            {new TypeLiteral<Map<String, String>>(){}, "Map<String, String>"},
//            {new TypeLiteral<Map.Entry<String, String>>(){}, "Map.Entry<String, String>"},
//            {new TypeLiteral<Set<Map.Entry<String, String>>>(){}, "Set<Map.Entry<String, String>>"},
//            {new TypeLiteral<Provider<String>>(){}, "Provider<String>"},
//        };
//    }
//
//    @Test(dataProvider = "genericTypeNameProvider")
//    public void testGenericTypeName(final TypeLiteral<?> typeLiteral, final String expectedName) {
//        final ServiceGraphVertex vertex = ServiceGraphVertex.newVertex(Key.get(typeLiteral), null);
//        assertThat(vertex.genericTypeName(typeLiteral), is(expectedName));
//    }

    @DataProvider
    Object[][] typeLiteralNameProvider() {
        return new Object[][] {
            {TypeLiteral.get(String.class), "String"},
            {new TypeLiteral<Set<Map.Entry<String, String>>>(){}, "Set<Map.Entry<String, String>>"},
        };
    }

    @Test(dataProvider = "typeLiteralNameProvider")
    public void testTypeLiternalName(final TypeLiteral<?> typeLiteral, final String expectedName) {
        final ServiceGraphVertex vertex = ServiceGraphVertex.newVertex(Key.get(typeLiteral),null);
        assertThat(vertex.typeLiteralName(typeLiteral), is(expectedName));
    }
}
