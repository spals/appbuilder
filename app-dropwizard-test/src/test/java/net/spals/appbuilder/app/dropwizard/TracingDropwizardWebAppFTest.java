package net.spals.appbuilder.app.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import net.spals.appbuilder.app.dropwizard.tracing.TracingDropwizardWebApp;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Functional tests for a minimal {@link DropwizardWebApp}
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
    public void testServerRequestTracing(final String path,
                                         final String expectedOperationName) {
        final String target = "http://localhost:" + testServerWrapper.getLocalPort() + "/" + path;
        final WebTarget webTarget = webClient.target(target);
        webTarget.request().get();

        final MockSpan mockSpan = findMockSpan(expectedOperationName);
        assertThat(mockSpan.generatedErrors(), empty());
        assertThat(mockSpan.tags(), hasEntry("http.method", "GET"));
        assertThat(mockSpan.tags(), hasEntry("http.status_code", 200));
        assertThat(mockSpan.tags(), hasEntry("http.url", target));
        assertThat(mockSpan.tags(), hasEntry("span.kind", "server"));
    }

    private MockSpan findMockSpan(final String expectedOperationName) {
        final List<MockSpan> finishedSpans = mockTracer.finishedSpans();
        assertThat("No finished spans found.", finishedSpans, hasSize(greaterThan(0)));

        final Optional<MockSpan> mockSpan = finishedSpans.stream()
                .filter(span -> expectedOperationName.equals(span.operationName()))
                .findAny();
        assertThat("Could not find span with operationName '" + expectedOperationName + "'",
                mockSpan, not(Optional.empty()));
        return mockSpan.get();
    }
}
