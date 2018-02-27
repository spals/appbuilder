package net.spals.appbuilder.app.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import net.spals.appbuilder.app.dropwizard.doc.DocDropwizardWebApp;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Functional tests for API documentation in a {@link DropwizardWebApp}
 * (see {@link DocDropwizardWebApp}).
 *
 * @author tkral
 */
public class DocDropwizardWebAppFTest {

    private final DropwizardTestSupport<Configuration> testServerWrapper =
        new DropwizardTestSupport<>(DocDropwizardWebApp.class, new Configuration());

    private final Client webClient = ClientBuilder.newClient();

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
    }

    @Test
    public void testApiDocumentation() {
        final String target = "http://localhost:" + testServerWrapper.getLocalPort() + "/swagger";
        final WebTarget webTarget = webClient.target(target);
        final Response docResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(docResponse.getStatus(), is(OK.getStatusCode()));
        final Map<String, Object> json = docResponse.readEntity(new GenericType<Map<String, Object>>() {});

        assertThat(json, hasKey("info"));
        final Map<String, Object> info = (Map<String, Object>)json.get("info");
        assertThat(info, hasEntry("title", "DocDropwizardWebApp API"));

        assertThat(json, hasKey("paths"));
        final Map<String, Object> paths = (Map<String, Object>)json.get("paths");
        assertThat(paths, hasKey("/doc/get"));
    }
}
