package net.spals.appbuilder.app.examples.dropwizard.tracing;

import io.opentracing.contrib.jaxrs2.server.Traced;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * A sample web resource.
 *
 * @author tkral
 */
@AutoBindSingleton
@Api(value = "tracing")
@Path("tracing")
public class TracingDropwizardResource {

    TracingDropwizardResource() { }

    @GET
    @Path("noAnnotation")
    @ApiOperation(value = "Execute tracing with no annotation")
    public Response getTracingNoAnnotation() {
        return Response.ok().build();
    }

    @GET
    @Path("noAnnotation/{id}")
    @ApiOperation(value = "Execute tracing with no annotation and an id")
    public Response getTrachingNoAnnotation(@PathParam("id") final String id) {
        return Response.ok().build();
    }

    @GET
    @Path("withAnnotation")
    @ApiOperation(value = "Execute tracing with an annotation")
    @Traced(operationName = "customOperationName")
    public Response getTracingWithAnnotation() {
        return Response.ok().build();
    }
}
