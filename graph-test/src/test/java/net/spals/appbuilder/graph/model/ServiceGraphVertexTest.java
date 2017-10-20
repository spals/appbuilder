package net.spals.appbuilder.graph.model;

import com.google.common.base.CharMatcher;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import net.spals.appbuilder.annotations.config.ApplicationName;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.spals.appbuilder.graph.model.ServiceGraphVertex.createGraphVertex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link ServiceGraphVertex}.
 *
 * @author tkral
 */
public class ServiceGraphVertexTest {

    @DataProvider
    Object[][] canPrintConstantProvider() {
        return new Object[][] {
            // Case: Small string
            {"1", true},
            // Case: String at limit
            {String.join("", Collections.nCopies(63, "1")), true},
            // Case: String is too long
            {String.join("", Collections.nCopies(64, "1")), false},
            // Case: Non-constant
            {new ServiceGraphVertexTest(), false},
        };
    }

    @Test(dataProvider = "canPrintConstantProvider")
    public <T> void testCanPrintConstant(final T serviceInstance, final boolean expectedResult) {
        final Key<T> key = Key.get((Class<T>) serviceInstance.getClass());
        final ServiceGraphVertex vertex = createGraphVertex(key, serviceInstance);

        assertThat(vertex.canPrintConstant(serviceInstance), is(expectedResult));
    }

    @DataProvider
    Object[][] equalsProvider() {
        return new Object[][] {
            // Case: Non-Vertex objects
            {createGraphVertex(Key.get(String.class), ""), null, false},
            {createGraphVertex(Key.get(String.class), ""), new Object(), false},
            // Case: Same key types, same instances
            {createGraphVertex(Key.get(String.class), ""),
                createGraphVertex(Key.get(String.class), ""), true},
            // Case: Different instances
            {createGraphVertex(Key.get(String.class), ""),
                createGraphVertex(Key.get(String.class), "0"), false},
            // Case: Different key types
            {createGraphVertex(Key.get(String.class), ""),
                createGraphVertex(Key.get(Integer.class), 0), false},
            // Case: Mismatched annotations
            {createGraphVertex(Key.get(String.class, ApplicationName.class), ""),
                createGraphVertex(Key.get(String.class), ""), false},
            // Case: Same key types, same annotations, same instances
            {createGraphVertex(Key.get(String.class, ApplicationName.class), ""),
                createGraphVertex(Key.get(String.class, ApplicationName.class), ""), true},
        };
    }

    @Test(dataProvider = "equalsProvider")
    public void testEquals(final ServiceGraphVertex vertex, final Object obj, final boolean expectedResult) {
        assertThat(vertex.equals(obj), is(expectedResult));
    }

    @Test(dataProvider = "equalsProvider")
    public void testHashCode(final ServiceGraphVertex vertex, final Object obj, final boolean expectedResult) {
        if (obj != null && obj instanceof ServiceDAGVertex) {
            final Matcher<Integer> hashCodeMatcher = expectedResult ? is(vertex.hashCode()) : not(vertex.hashCode());
            assertThat(obj.hashCode(), hashCodeMatcher);
        }
    }

    @Test
    public void testInHashSet() {
        final HashSet<ServiceGraphVertex> vertexSet = new HashSet<>();
        vertexSet.add(createGraphVertex(Key.get(String.class), ""));
        vertexSet.add(createGraphVertex(Key.get(String.class), ""));

        assertThat(vertexSet, hasSize(1));
    }

    @DataProvider
    Object[][] isConstantProvider() {
        return new Object[][] {
            // Case: Strings are constants
            {"123", true},
            //Case: Paths are constants
            {Paths.get("path"), true},
            // Case: Booleans are constants
            {Boolean.valueOf(false), true},
            // Case: Numbers are constants
            {Double.valueOf(0.0d), true},
            {Integer.valueOf(0), true},
            {Float.valueOf(0.0f), true},
            {Long.valueOf(0L), true},
            // Case: Objects are not constants
            {new ServiceGraphVertexTest(), false},
        };
    }

    @Test(dataProvider = "isConstantProvider")
    public <T> void testIsConstant(final T serviceInstance, final boolean expectedResult) {
        final Key<T> key = Key.get((Class<T>) serviceInstance.getClass());
        final ServiceGraphVertex vertex = createGraphVertex(key, serviceInstance);

        assertThat(vertex.isConstant(serviceInstance), is(expectedResult));
    }

    @DataProvider
    Object[][] genericTypeNameProvider() {
        return new Object[][] {
            {new TypeLiteral<Set<String>>(){}, "Set<String>"},
            {new TypeLiteral<Set<ServiceGraphVertexTest>>(){},
                "Set<ServiceGraphVertexTest>"},
            {new TypeLiteral<Map<String, String>>(){},
                "Map<String, String>"},
            {new TypeLiteral<Map.Entry<String, String>>(){},
                "Map.Entry<String, String>"},
            {new TypeLiteral<Set<Map.Entry<String, String>>>(){},
                "Set<Map.Entry<String, String>>"},
            {new TypeLiteral<Provider<String>>(){}, "Provider<String>"},
        };
    }

    @Test(dataProvider = "genericTypeNameProvider")
    public void testGenericTypeName(final TypeLiteral<Object> typeLiteral,
                                    final String expectedName) {
        assertThat(ServiceGraphVertex.genericTypeName(typeLiteral), is(expectedName));
    }

    @DataProvider
    Object[][] simpleTypeNameProvider() {
        return new Object[][] {
            {String.class, "String"},
            {String[].class, "String[]"},
            {ServiceGraphVertexTest.class, "ServiceGraphVertexTest"},
            {ServiceGraphVertexTest[].class, "ServiceGraphVertexTest[]"},
            {CharMatcher.class, "com.google.common.base.CharMatcher"},
        };
    }

    @Test(dataProvider = "simpleTypeNameProvider")
    public void testSimpleTypeName(final Class<? extends Object> simpleType,
                                   final String expectedName) {
        assertThat(ServiceGraphVertex.simpleTypeName(simpleType), is(expectedName));
    }

    @DataProvider
    Object[][] toStringProvider() {
        return new Object[][] {
            // Case: Constant
            {createGraphVertex(Key.get(String.class), "1"),
                "\"1\""},
            // Case: Constant with annotation
            {createGraphVertex(Key.get(String.class, Names.named("constant")), "1"),
                "@Named(constant) \"1\""},
            // Case: Service
            {createGraphVertex(Key.get(ServiceGraphVertexTest.class), new ServiceGraphVertexTest()),
                "ServiceGraphVertexTest"},
            // Case: Service with annotation
            {createGraphVertex(Key.get(ServiceGraphVertexTest.class, Names.named("service")),
                new ServiceGraphVertexTest()),
                "@Named(service) ServiceGraphVertexTest"},
        };
    }

    @Test(dataProvider = "toStringProvider")
    public void testToString(final ServiceGraphVertex<?> vertex, final String expectedResult) {
        assertThat(vertex.toString(), is(expectedResult));
    }

    @DataProvider
    Object[][] typeLiteralNameProvider() {
        return new Object[][] {
            {TypeLiteral.get(String.class), "String"},
            {new TypeLiteral<Set<Map.Entry<String, String>>>(){},
                "Set<Map.Entry<String, String>>"},
        };
    }

    @Test(dataProvider = "typeLiteralNameProvider")
    public void testTypeLiteralName(final TypeLiteral<Object> typeLiteral,
                                    final String expectedName) {
        assertThat(ServiceGraphVertex.typeLiteralName(typeLiteral), is(expectedName));
    }
}
