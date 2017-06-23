package net.spals.appbuilder.app.core.sample.web;

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
@Path("myPath")
public class SampleCoreResource {

    SampleCoreResource() { }

    @GET
    public Response getCallback() {
        return Response.ok().build();
    }
}
