package net.spals.appbuilder.graph.model;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static net.spals.appbuilder.graph.model.PrintableVertex.createPrintableVertex;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createDAGVertex;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createDAGVertexWithProvider;
import static net.spals.appbuilder.graph.model.ServiceGraphVertex.createGraphVertex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link PrintableVertex}.
 *
 * @author tkral
 */
public class PrintableVertexTest {

    @Test
    public void tetGetGuiceKey() {
        final IServiceGraphVertex vertex = Mockito.mock(IServiceGraphVertex.class);
        final PrintableVertex printableVertex = createPrintableVertex(vertex, " ");

        printableVertex.getGuiceKey();
        verify(vertex).getGuiceKey();
    }

    @Test
    public void tetGetServiceInstance() {
        final IServiceGraphVertex vertex = Mockito.mock(IServiceGraphVertex.class);
        final PrintableVertex printableVertex = createPrintableVertex(vertex, " ");

        printableVertex.getServiceInstance();
        verify(vertex).getServiceInstance();
    }

    @DataProvider
    Object[][] toStringProvider() {
        return new Object[][] {
            {createGraphVertex(Key.get(String.class), "1"), " ", "\"1\""},
            {createGraphVertex(Key.get(String.class, Names.named("constant")), "1"), "~",
                "@Named(constant)~\"1\""},
            {createDAGVertexWithProvider(Key.get(PrintableVertexTest.class), new PrintableVertexTest(),
                createDAGVertex(Key.get(Provider.class), new Provider<PrintableVertexTest>() {
                    @Override
                    public PrintableVertexTest get() {
                        return new PrintableVertexTest();
                    }
                })), "~",
                "PrintableVertexTest~[Provider:Provider]"},

        };
    }

    @Test(dataProvider = "toStringProvider")
    public void testToString(final IServiceGraphVertex<?> vertex,
                             final String separator,
                             final String expectedResult) {
        final PrintableVertex printableVertex = createPrintableVertex(vertex, separator);
        assertThat(printableVertex.toString(), is(expectedResult));
    }
}
