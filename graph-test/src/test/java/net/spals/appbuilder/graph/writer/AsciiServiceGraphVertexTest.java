package net.spals.appbuilder.graph.writer;

import com.google.common.base.CharMatcher;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import net.spals.appbuilder.graph.model.ServiceGraphVertex;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AsciiServiceGraphVertex}.
 *
 * @author tkral
 */
public class AsciiServiceGraphVertexTest {

    @DataProvider
    Object[][] simpleTypeNameProvider() {
        return new Object[][] {
                {String.class, "String"},
                {String[].class, "String[]"},
                {AsciiServiceGraphVertexTest.class, "AsciiServiceGraphVertexTest"},
                {AsciiServiceGraphVertexTest[].class, "AsciiServiceGraphVertexTest[]"},
                {CharMatcher.class, "com.google.common.base.CharMatcher"},
        };
    }

    @Test(dataProvider = "simpleTypeNameProvider")
    public void testSimpleTypeName(final Class<?> simpleType, final String expectedName) {
        final AsciiServiceGraphVertex asciiVertex = new AsciiServiceGraphVertex(mock(ServiceGraphVertex.class));
        assertThat(asciiVertex.simpleTypeName(simpleType), is(expectedName));
    }

    @DataProvider
    Object[][] genericTypeNameProvider() {
        return new Object[][] {
                {new TypeLiteral<Set<String>>(){}, "Set<String>"},
                {new TypeLiteral<Set<AsciiServiceGraphVertexTest>>(){}, "Set<AsciiServiceGraphVertexTest>"},
                {new TypeLiteral<Map<String, String>>(){}, "Map<String, String>"},
                {new TypeLiteral<Map.Entry<String, String>>(){}, "Map.Entry<String, String>"},
                {new TypeLiteral<Set<Map.Entry<String, String>>>(){}, "Set<Map.Entry<String, String>>"},
                {new TypeLiteral<Provider<String>>(){}, "Provider<String>"},
        };
    }

    @Test(dataProvider = "genericTypeNameProvider")
    public void testGenericTypeName(final TypeLiteral<?> typeLiteral, final String expectedName) {
        final AsciiServiceGraphVertex asciiVertex = new AsciiServiceGraphVertex(mock(ServiceGraphVertex.class));
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
        final AsciiServiceGraphVertex asciiVertex = new AsciiServiceGraphVertex(mock(ServiceGraphVertex.class));
        assertThat(asciiVertex.typeLiteralName(typeLiteral), is(expectedName));
    }
}
