package net.spals.appbuilder.app.dropwizard.tracing;

import io.opentracing.contrib.jaxrs2.server.Traced;
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
@Path("tracing")
public class TracingDropwizardResource {

    TracingDropwizardResource() { }

    @GET
    @Path("noAnnotation")
    public Response getTracingNoAnnotation() {
        return Response.ok().build();
    }

    @GET
    @Path("noAnnotation/{id}")
    public Response getTrachingNoAnnotation(@PathParam("id") final String id) {
        return Response.ok().build();
    }

    @GET
    @Path("withAnnotation")
    @Traced(operationName = "customOperationName")
    public Response getTracingWithAnnotation() {
        return Response.ok().build();
    }
}
