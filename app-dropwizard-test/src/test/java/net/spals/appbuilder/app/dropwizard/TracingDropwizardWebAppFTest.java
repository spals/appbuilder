package net.spals.appbuilder.app.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import net.spals.appbuilder.app.examples.dropwizard.tracing.TracingDropwizardWebApp;
import org.testng.annotations.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Functional tests for request tracing in a {@link DropwizardWebApp}
 * (see {@link TracingDropwizardWebApp}).
 *
 * @author tkral
 */
public class TracingDropwizardWebAppFTest {

    private final DropwizardTestSupport<Configuration> testServerWrapper =
        new DropwizardTestSupport<>(TracingDropwizardWebApp.class, new Configuration());
    private MockTracer mockTracer;

    private final Client webClient = ClientBuilder.newClient();

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
        mockTracer = ((TracingDropwizardWebApp)testServerWrapper.getApplication()).getMockTracer();
    }

    @BeforeMethod
    void resetTracer() {
        mockTracer.reset();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
    }

    @DataProvider
    Object[][] serverRequestTracingProvider() {
        return new Object[][] {
            {"tracing/noAnnotation", "tracing/noAnnotation"},
            {"tracing/withAnnotation", "customOperationName"},
        };
    }

    @Test(dataProvider = "serverRequestTracingProvider")
    public void testServerRequestTracing(
        final String path,
        final String expectedOperationName
    ) {
        final String target = "http://localhost:" + testServerWrapper.getLocalPort() + "/" + path;
        final WebTarget webTarget = webClient.target(target);
        webTarget.request().get();

        final List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat("No finished spans found.", mockSpans, hasSize(1));

        final MockSpan mockSpan = mockSpans.get(0);
        assertThat(mockSpan.generatedErrors(), empty());
        assertThat(mockSpan.operationName(), is(expectedOperationName));
        assertThat(mockSpan.tags(), hasEntry("http.method", "GET"));
        assertThat(mockSpan.tags(), hasEntry("http.status_code", 200));
        assertThat(mockSpan.tags(), hasEntry("http.url", target));
        assertThat(mockSpan.tags(), hasEntry("span.kind", "server"));
    }

    @Test
    public void testServerRequestTracingWithParams() {
        final String target = "http://localhost:" + testServerWrapper.getLocalPort() + "/tracing/noAnnotation/123";
        final WebTarget webTarget = webClient.target(target);
        webTarget.request().get();

        final List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertThat("No finished spans found.", mockSpans, hasSize(1));

        final MockSpan mockSpan = mockSpans.get(0);
        assertThat(mockSpan.generatedErrors(), empty());
        assertThat(mockSpan.operationName(), is("tracing/noAnnotation/{id}"));
        assertThat(mockSpan.tags(), hasEntry("http.method", "GET"));
        assertThat(mockSpan.tags(), hasEntry("http.status_code", 200));
        assertThat(mockSpan.tags(), hasEntry("http.url", target));
        assertThat(mockSpan.tags(), hasEntry("param.id", "123"));
        assertThat(mockSpan.tags(), hasEntry("span.kind", "server"));
    }
}
