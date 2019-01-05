package net.spals.appbuilder.app.dropwizard;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import net.spals.appbuilder.app.examples.dropwizard.cors.CorsDropwizardWebApp;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Functional tests for CORS registration in a {@link DropwizardWebApp}
 * (see {@link CorsDropwizardWebApp}).
 *
 * @author tkral
 */
public class CorsDropwizardWebAppFTest {

    private final DropwizardTestSupport<Configuration> testServerWrapper =
        new DropwizardTestSupport<>(
            CorsDropwizardWebApp.class,
            "",
            ConfigOverride.config("server.applicationConnectors[0].port", "0")
        );

    private final Client webClient = ClientBuilder.newClient();

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
    }

    // Force the DockDropwizardWebApp to launch first. This is because Swagger uses
    // static variables to store API metadata, which testApiDocumentation attempts
    // to assert. If this class is run first (as the TestNG default seems to be execute
    // tests alphabetically by class name), then then static state is set incorrectly
    // and the testApiDocumentation assert fails.
    @Test(dependsOnMethods="net.spals.appbuilder.app.dropwizard.DocDropwizardWebAppFTest.testApiDocumentation")
    public void testCorsEnabled() {
        final String target = "http://localhost:" + testServerWrapper.getLocalPort() + "/cors/get";
        final WebTarget webTarget = webClient.target(target);
        final Response corsResponse = webTarget.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(corsResponse.getStatus(), is(OK.getStatusCode()));
        assertThat(corsResponse.getStringHeaders(), hasEntry(is("Access-Control-Allow-Origin"), contains("*")));
        assertThat(corsResponse.getStringHeaders(), hasEntry(is("Access-Control-Allow-Credentials"), contains("true")));
        assertThat(corsResponse.getStringHeaders(),
            hasEntry(is("Access-Control-Allow-Headers"), contains("origin, content-type, accept, authorization")));
        assertThat(corsResponse.getStringHeaders(),
            hasEntry(is("Access-Control-Allow-Methods"), contains("GET, POST, PUT, DELETE, OPTIONS, HEAD")));
    }
}
