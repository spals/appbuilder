package net.spals.appbuilder.graph.model;

import com.google.inject.Key;
import com.google.inject.Provider;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createDAGVertex;
import static net.spals.appbuilder.graph.model.ServiceDAGVertex.createDAGVertexWithProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link ServiceDAGVertex}.
 *
 * @author tkral
 */
public class ServiceDAGVertexTest {

    @DataProvider
    Object[][] toStringProvider() {
        return new Object[][] {
            // Case: Service with provider
            {createDAGVertexWithProvider(Key.get(ServiceDAGVertexTest.class), new ServiceDAGVertexTest(),
                createDAGVertex(Key.get(Provider.class), new Provider<ServiceDAGVertexTest>() {
                    @Override
                    public ServiceDAGVertexTest get() {
                        return new ServiceDAGVertexTest();
                    }
                })),
                "ServiceDAGVertexTest [Provider:Provider]"},
        };
    }

    @Test(dataProvider = "toStringProvider")
    public void testToString(final ServiceDAGVertex<?> vertex, final String expectedResult) {
        assertThat(vertex.toString(), is(expectedResult));
    }
}
