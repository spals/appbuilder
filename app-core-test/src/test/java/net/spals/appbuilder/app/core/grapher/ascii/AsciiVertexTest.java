package net.spals.appbuilder.app.core.grapher.ascii;

import com.google.common.base.CharMatcher;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.grapher.Node;
import net.spals.appbuilder.app.core.grapher.ServiceGrapher;
import net.spals.appbuilder.app.core.grapher.ServiceGrapher.Vertex;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AsciiVertex}.
 *
 * @author tkral
 */
public class AsciiVertexTest {

    @DataProvider
    Object[][] simpleTypeNameProvider() {
        return new Object[][] {
                {String.class, "String"},
                {String[].class, "String[]"},
                {AsciiVertexTest.class, "AsciiVertexTest"},
                {AsciiVertexTest[].class, "AsciiVertexTest[]"},
                {CharMatcher.class, "com.google.common.base.CharMatcher"},
        };
    }

    @Test(dataProvider = "simpleTypeNameProvider")
    public void testSimpleTypeName(final Class<?> simpleType, final String expectedName) {
        final AsciiVertex asciiVertex = new AsciiVertex(mock(Vertex.class));
        assertThat(asciiVertex.simpleTypeName(simpleType), is(expectedName));
    }

    @DataProvider
    Object[][] genericTypeNameProvider() {
        return new Object[][] {
                {new TypeLiteral<Set<String>>(){}, "Set<String>"},
                {new TypeLiteral<Set<AsciiVertexTest>>(){}, "Set<AsciiVertexTest>"},
                {new TypeLiteral<Map<String, String>>(){}, "Map<String, String>"},
                {new TypeLiteral<Map.Entry<String, String>>(){}, "Map.Entry<String, String>"},
                {new TypeLiteral<Set<Map.Entry<String, String>>>(){}, "Set<Map.Entry<String, String>>"},
                {new TypeLiteral<Provider<String>>(){}, "Provider<String>"},
        };
    }

    @Test(dataProvider = "genericTypeNameProvider")
    public void testGenericTypeName(final TypeLiteral<?> typeLiteral, final String expectedName) {
        final AsciiVertex asciiVertex = new AsciiVertex(mock(Vertex.class));
        assertThat(asciiVertex.genericTypeName(typeLiteral), is(expectedName));
    }

    @DataProvider
    Object[][] typeLiteralNameProvider() {
        return new Object[][] {
                {TypeLiteral.get(String.class), "String"},
                {new TypeLiteral<Set<Map.Entry<String, String>>>(){}, "Set<Map.Entry<String, String>>"},
        };
    }

    @Test(dataProvider = "typeLiteralNameProvider")
    public void testTypeLiternalName(final TypeLiteral<?> typeLiteral, final String expectedName) {
        final AsciiVertex asciiVertex = new AsciiVertex(mock(Vertex.class));
        assertThat(asciiVertex.typeLiteralName(typeLiteral), is(expectedName));
    }
}
