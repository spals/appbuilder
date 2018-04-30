package net.spals.appbuilder.app.examples.dropwizard.cors;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * A web resource for testing CORS.
 *
 * @author tkral
 */
@AutoBindSingleton
@Path("cors")
public class CorsDropwizardResource {

    CorsDropwizardResource() { }

    @GET
    @Path("get")
    public Response getCallback() {
        return Response.ok().build();
    }

}
