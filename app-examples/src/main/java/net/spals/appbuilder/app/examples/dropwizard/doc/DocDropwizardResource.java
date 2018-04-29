package net.spals.appbuilder.app.examples.dropwizard.doc;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * A Dropwizard web resource with API documentation.
 *
 * @author tkral
 */
@AutoBindSingleton
@Api(value = "doc")
@Path("doc")
public class DocDropwizardResource {

    DocDropwizardResource() { }

    @GET
    @Path("get")
    @ApiOperation(value = "Execute get request with no parameters")
    public Response getCallback() {
        return Response.ok().build();
    }

    @GET
    @Path("get/{id}")
    @ApiOperation(value = "Execute get request with single id parameter")
    public Response getCallback(@PathParam("id") final String id) {
        return Response.ok().build();
    }
}
