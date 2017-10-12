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
import java.util.*;

import static net.spals.appbuilder.graph.model.ServiceDAGVertex.newVertex;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.vertexWithProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link ServiceDAGVertex}.
 *
 * @author tkral
 */
public class ServiceDAGVertexTest {

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
            {new ServiceDAGVertexTest(), false},
        };
    }

    @Test(dataProvider = "canPrintConstantProvider")
    public <T> void testCanPrintConstant(final T serviceInstance, final boolean expectedResult) {
        final Key<T> key = Key.get((Class<T>) serviceInstance.getClass());
        final ServiceDAGVertex vertex = newVertex(key, serviceInstance);

        assertThat(vertex.canPrintConstant(serviceInstance), is(expectedResult));
    }

    @DataProvider
    Object[][] equalsProvider() {
        return new Object[][] {
            // Case: Non-Vertex objects
            {newVertex(Key.get(String.class), ""), null, false},
            {newVertex(Key.get(String.class), ""), new Object(), false},
            // Case: Same key types, same instances
            {newVertex(Key.get(String.class), ""),
                newVertex(Key.get(String.class), ""), true},
            // Case: Different instances
            {newVertex(Key.get(String.class), ""),
                newVertex(Key.get(String.class), "0"), false},
            // Case: Different key types
            {newVertex(Key.get(String.class), ""),
                newVertex(Key.get(Integer.class), 0), false},
            // Case: Mismatched annotations
            {newVertex(Key.get(String.class, ApplicationName.class), ""),
                newVertex(Key.get(String.class), ""), false},
            // Case: Same key types, same annotations, same instances
            {newVertex(Key.get(String.class, ApplicationName.class), ""),
                newVertex(Key.get(String.class, ApplicationName.class), ""), true},
        };
    }

    @Test(dataProvider = "equalsProvider")
    public void testEquals(final ServiceDAGVertex vertex, final Object obj, final boolean expectedResult) {
        assertThat(vertex.equals(obj), is(expectedResult));
    }

    @Test(dataProvider = "equalsProvider")
    public void testHashCode(final ServiceDAGVertex vertex, final Object obj, final boolean expectedResult) {
        if (obj != null && obj instanceof ServiceDAGVertex) {
            final Matcher<Integer> hashCodeMatcher = expectedResult ? is(vertex.hashCode()) : not(vertex.hashCode());
            assertThat(obj.hashCode(), hashCodeMatcher);
        }
    }

    @Test
    public void testInHashSet() {
        final HashSet<ServiceDAGVertex> vertexSet = new HashSet<>();
        vertexSet.add(newVertex(Key.get(String.class), ""));
        vertexSet.add(newVertex(Key.get(String.class), ""));

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
            {new ServiceDAGVertexTest(), false},
        };
    }

    @Test(dataProvider = "isConstantProvider")
    public <T> void testIsConstant(final T serviceInstance, final boolean expectedResult) {
        final Key<T> key = Key.get((Class<T>) serviceInstance.getClass());
        final ServiceDAGVertex vertex = newVertex(key, serviceInstance);

        assertThat(vertex.isConstant(serviceInstance), is(expectedResult));
    }

    @DataProvider
    Object[][] genericTypeNameProvider() {
        return new Object[][] {
            {new TypeLiteral<Set<String>>(){},
                Collections.<String>emptySet(),
                "Set<String>"},
            {new TypeLiteral<Set<ServiceDAGVertexTest>>(){},
                Collections.<ServiceDAGVertexTest>emptySet(),
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
        final ServiceDAGVertex vertex = newVertex(Key.get(typeLiteral), serviceInstance);
        assertThat(vertex.genericTypeName(typeLiteral), is(expectedName));
    }

    @DataProvider
    Object[][] simpleTypeNameProvider() {
        return new Object[][] {
            {String.class, "", "String"},
            {String[].class, new String[0], "String[]"},
            {ServiceDAGVertexTest.class, new ServiceDAGVertexTest(), "ServiceGraphVertexTest"},
            {ServiceDAGVertexTest[].class, new ServiceDAGVertexTest[0], "ServiceGraphVertexTest[]"},
            {CharMatcher.class, CharMatcher.any(), "com.google.common.base.CharMatcher"},
        };
    }

    @Test(dataProvider = "simpleTypeNameProvider")
    public void testSimpleTypeName(final Class<? extends Object> simpleType,
                                   final Object serviceInstance,
                                   final String expectedName) {
        final TypeLiteral<? extends Object> typeLiteral = TypeLiteral.get(simpleType);
        final Key<Object> key = (Key<Object>) Key.get(typeLiteral);
        final ServiceDAGVertex vertex = newVertex(key, serviceInstance);
        assertThat(vertex.simpleTypeName(simpleType), is(expectedName));
    }

    @DataProvider
    Object[][] toStringProvider() {
        return new Object[][] {
            // Case: Constant
            {newVertex(Key.get(String.class), "1"),
                "\"1\""},
            // Case: Constant with annotation
            {newVertex(Key.get(String.class, Names.named("constant")), "1"),
                "@Named(constant) \"1\""},
            // Case: Service
            {newVertex(Key.get(ServiceDAGVertexTest.class), new ServiceDAGVertexTest()),
                "ServiceGraphVertexTest"},
            // Case: Service with annotation
            {newVertex(Key.get(ServiceDAGVertexTest.class, Names.named("service")),
                new ServiceDAGVertexTest()),
                "@Named(service) ServiceGraphVertexTest"},
            // Case: Service with provider
            {vertexWithProvider(newVertex(Key.get(ServiceDAGVertexTest.class), new ServiceDAGVertexTest()),
                newVertex(Key.get(Provider.class), new Provider<ServiceDAGVertexTest>() {
                    @Override
                    public ServiceDAGVertexTest get() {
                        return new ServiceDAGVertexTest();
                    }
                })),
                "ServiceGraphVertexTest [Provider:Provider]"},
        };
    }

    @Test(dataProvider = "toStringProvider")
    public void testToString(final ServiceDAGVertex<?> vertex, final String expectedResult) {
        assertThat(vertex.toString(), is(expectedResult));
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
        final ServiceDAGVertex vertex = newVertex(Key.get(typeLiteral), serviceInstance);
        assertThat(vertex.typeLiteralName(typeLiteral), is(expectedName));
    }
}
