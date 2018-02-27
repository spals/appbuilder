package net.spals.appbuilder.app.dropwizard.doc;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * A sample web resource.
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
    @ApiOperation(value = "Execute tracing with no annotation")
    public Response getCallback() {
        return Response.ok().build();
    }
}
