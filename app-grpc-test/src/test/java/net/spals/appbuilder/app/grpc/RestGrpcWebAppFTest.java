package net.spals.appbuilder.app.grpc;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.ImmutableMap;
import net.spals.appbuilder.app.grpc.rest.*;
import net.spals.appbuilder.app.grpc.rest.UserServiceGrpc.UserServiceBlockingStub;
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

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
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

    private String grpcUserId;
    private String restUserId;

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
    public void testGrpcPostRequest() {
        final UserServiceBlockingStub stub =
            UserServiceGrpc.newBlockingStub(testServerWrapper.getChannel());

        final PostUserRequest request = PostUserRequest.newBuilder()
            .setName("Tim").build();
        final PostUserResponse response = stub.postUser(request);

        assertThat(response.getId(), notNullValue());
        assertThat(response.getName(), is("Tim"));
        grpcUserId = response.getId();
    }

    @Test(dependsOnMethods = "testGrpcPostRequest")
    public void testGrpcGetRequest() {
        final UserServiceBlockingStub stub =
            UserServiceGrpc.newBlockingStub(testServerWrapper.getChannel());

        final GetUserRequest request = GetUserRequest.newBuilder()
            .setId(grpcUserId).build();
        final GetUserResponse response = stub.getUser(request);

        assertThat(response.getId(), is(grpcUserId));
        assertThat(response.getName(), is("Tim"));
    }

    @Test(dependsOnMethods = "testGrpcGetRequest")
    public void testGrpcDeleteRequest() {
        final UserServiceBlockingStub stub =
            UserServiceGrpc.newBlockingStub(testServerWrapper.getChannel());

        final DeleteUserRequest request = DeleteUserRequest.newBuilder()
            .setId(grpcUserId).build();
        final DeleteUserResponse response = stub.deleteUser(request);

        assertThat(response.getId(), is(grpcUserId));
    }

    @Test
    public void testRestPostRequest() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/users";
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ImmutableMap.of("name", "Tim")));

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), notNullValue()));
        restUserId = json.get("id").toString();
    }

//    @Test
//    public void testRestGetRequestMissingData() {
//        final String target = "http://localhost:" + restApp.getRestPort() + "/users/deadbeef";
//        final WebTarget restTarget = restClient.target(target);
//        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE).get();
//
//        assertThat(restResponse.getStatus(), is(NOT_FOUND.getStatusCode()));
//    }

    @Test(dependsOnMethods = "testRestPostRequest")
    public void testRestGetRequest() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/users/" + restUserId;
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), is(restUserId)));
        assertThat(json, hasEntry(is("name"), is("Tim")));
    }

    @Test(dependsOnMethods = "testRestGetRequest")
    public void testRestDeleteRequest() {
        final String target = "http://localhost:" + restApp.getRestPort() + "/users/" + restUserId;
        final WebTarget restTarget = restClient.target(target);
        final Response restResponse = restTarget.request(MediaType.APPLICATION_JSON_TYPE).delete();

        assertThat(restResponse.getStatus(), is(OK.getStatusCode()));
        final Map<String, Object> json = restResponse.readEntity(new GenericType<Map<String, Object>>() {});
        assertThat(json, hasEntry(is("id"), is(restUserId)));
    }
}
