package net.spals.appbuilder.app.grpc;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.ImmutableMap;
import net.spals.appbuilder.app.examples.grpc.rest.*;
import net.spals.appbuilder.app.examples.grpc.rest.UserServiceV2Grpc.UserServiceV2BlockingStub;
import net.spals.appbuilder.app.examples.grpc.rest.UserServiceV3Grpc.UserServiceV3BlockingStub;
import net.spals.appbuilder.mapstore.core.MapStore;
import net.spals.appbuilder.mapstore.core.model.MapStoreTableKey;
import org.glassfish.jersey.client.ClientConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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

    private String grpcV2UserId;
    private String restV2UserId;
    private String grpcV3UserId;
    private String restV3UserId;

    @BeforeClass
    void classSetup() {
        testServerWrapper.before();

        final MapStore mapStore = restApp.getServiceInjector().getInstance(MapStore.class);
        mapStore.createTable("users", new MapStoreTableKey.Builder().setHash("id", String.class).build());
    }

    @AfterClass(alwaysRun = true)
    void classTearDown() {
        final MapStore mapStore = restApp.getServiceInjector().getInstance(MapStore.class);
        mapStore.dropTable("users");

        testServerWrapper.after();
    }

    @Test
    public void testGrpcPostRequestV2() {
        final UserServiceV2BlockingStub stub =
            UserServiceV2Grpc.newBlockingStub(testServerWrapper.getChannel());

        final PostUserRequestV2 request = PostUserRequestV2.newBuilder()
            .setName("Tim").build();
        final PostUserResponseV2 response = stub.postUserV2(request);

        assertThat(response.getId(), notNullValue());
        assertThat(response.getName(), is("Tim"));
        grpcV2UserId = response.getId();
    }

    @Test(dependsOnMethods = "testGrpcPostRequestV2")
    public void testGrpcGetRequestV2() {
        final UserServiceV2BlockingStub stub =
            UserServiceV2Grpc.newBlockingStub(testServerWrapper.getChannel());

        final GetUserRequestV2 request = GetUserRequestV2.newBuilder()
            .setId(grpcV2UserId).build();
        final GetUserResponseV2 response = stub.getUserV2(request);

        assertThat(response.getId(), is(grpcV2UserId));
        assertThat(response.getName(), is("Tim"));
    }

    @Test(dependsOnMethods = "testGrpcGetRequestV2")
    public void testGrpcDeleteRequestV2() {
        final UserServiceV2BlockingStub stub =
            UserServiceV2Grpc.newBlockingStub(testServerWrapper.getChannel());

        final DeleteUserRequestV2 request = DeleteUserRequestV2.newBuilder()
            .setId(grpcV2UserId).build();
        final DeleteUserResponseV2 response = stub.deleteUserV2(request);

        assertThat(response.getId(), is(grpcV2UserId));
    }

    @Test
    public void testGrpcPostRequestV3() {
        final UserServiceV3BlockingStub stub =
            UserServiceV3Grpc.newBlockingStub(testServerWrapper.getChannel());

        final PostUserRequestV3 request = PostUserRequestV3.newBuilder()
            .setName("Tim").build();
        final PostUserResponseV3 response = stub.postUserV3(request);

        assertThat(response.getId(), notNullValue());
        assertThat(response.getName(), is("Tim"));
        grpcV3UserId = response.getId();
    }

    @Test(dependsOnMethods = "testGrpcPostRequestV3")
    public void testGrpcGetRequestV3() {
        final UserServiceV3BlockingStub stub =
            UserServiceV3Grpc.newBlockingStub(testServerWrapper.getChannel());

        final GetUserRequestV3 request = GetUserRequestV3.newBuilder()
            .setId(grpcV3UserId).build();
        final GetUserResponseV3 response = stub.getUserV3(request);

        assertThat(response.getId(), is(grpcV3UserId));
        assertThat(response.getName(), is("Tim"));
    }

    @Test(dependsOnMethods = "testGrpcGetRequestV3")
    public void testGrpcDeleteRequestV3() {
        final UserServiceV3BlockingStub stub =
            UserServiceV3Grpc.newBlockingStub(testServerWrapper.getChannel());

        final DeleteUserRequestV3 request = DeleteUserRequestV3.newBuilder()
            .setId(grpcV3UserId).build();
        final DeleteUserResponseV3 response = stub.deleteUserV3(request);

        assertThat(response.getId(), is(grpcV3UserId));
    }

    @Test
    public void testRestPostRequestV2() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/v2/users";
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ImmutableMap.of("name", "Tim")));

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        assertCors(restResponse);
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), notNullValue()));
        restV2UserId = json.get("id").toString();
    }

    @Test(dependsOnMethods = "testRestPostRequestV2")
    public void testRestGetRequestV2() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/v2/users/" + restV2UserId;
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        assertCors(restResponse);
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), is(restV2UserId)));
        assertThat(json, hasEntry(is("name"), is("Tim")));
    }

    @Test(dependsOnMethods = "testRestGetRequestV2")
    public void testRestDeleteRequestV2() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/v2/users/" + restV2UserId;
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE).delete();

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        assertCors(restResponse);
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), is(restV2UserId)));
    }

    @Test
    public void testRestPostRequestV3() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/v3/users";
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ImmutableMap.of("name", "Tim")));

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        assertCors(restResponse);
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), notNullValue()));
        restV3UserId = json.get("id").toString();
    }

    @Test(dependsOnMethods = "testRestPostRequestV3")
    public void testRestGetRequestV3() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/v3/users/" + restV3UserId;
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        assertCors(restResponse);
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), is(restV3UserId)));
        assertThat(json, hasEntry(is("name"), is("Tim")));
    }

    @Test(dependsOnMethods = "testRestGetRequestV3")
    public void testRestDeleteRequestV3() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/v3/users/" + restV3UserId;
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE).delete();

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        assertCors(restResponse);
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), is(restV3UserId)));
    }

    private void assertCors(final Response response) {
        assertThat(response.getStringHeaders(), hasEntry(is("Access-Control-Allow-Origin"), contains("*")));
        assertThat(response.getStringHeaders(), hasEntry(is("Access-Control-Allow-Credentials"), contains("true")));
        assertThat(response.getStringHeaders(),
            hasEntry(is("Access-Control-Allow-Headers"), contains("origin, content-type, accept, authorization")));
        assertThat(response.getStringHeaders(),
            hasEntry(is("Access-Control-Allow-Methods"), contains("GET, POST, PUT, DELETE, OPTIONS, HEAD")));
    }
}
