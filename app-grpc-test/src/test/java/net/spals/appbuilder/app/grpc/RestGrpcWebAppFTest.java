package net.spals.appbuilder.app.grpc;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import net.spals.appbuilder.app.grpc.rest.RestGetRequest;
import net.spals.appbuilder.app.grpc.rest.RestGetResponse;
import net.spals.appbuilder.app.grpc.rest.RestGrpcWebApp;
import net.spals.appbuilder.app.grpc.rest.RestServiceGrpc;
import net.spals.appbuilder.app.grpc.rest.RestServiceGrpc.RestServiceBlockingStub;
import org.glassfish.jersey.client.ClientConfig;
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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;

/**
 * Functional tests for a {@link GrpcWebApp}
 * with RESTful APIs.
 *
 * @author tkral
 */
public class RestGrpcWebAppFTest {

    private final GrpcWebApp restApp = new RestGrpcWebApp();
    private final GrpcTestSupport testServerWrapper = GrpcTestSupport.nettyGrpc(restApp);

    private final Client restClient = ClientBuilder.newBuilder()
        .withConfig(new ClientConfig().register(JacksonJsonProvider.class))
        .build();

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();
    }

    @AfterClass
    void classTearDown() {
        testServerWrapper.after();
    }

    @Test
    public void testServiceRequestInGrpc() {
        final RestServiceBlockingStub stub =
            RestServiceGrpc.newBlockingStub(testServerWrapper.getChannel());

        final RestGetRequest request = RestGetRequest.newBuilder()
            .setId(1).build();
        final RestGetResponse response = stub.getRest(request);

        assertThat(response.getId(), is(3));
    }

    @Test
    public void testServiceRequestInRest() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/rest/1";
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), is(3)));
    }
}
