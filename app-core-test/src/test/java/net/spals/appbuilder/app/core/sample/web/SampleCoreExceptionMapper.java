package net.spals.appbuilder.app.core.sample.web;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.IOException;

/**
 * A sample web exception mapper.
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleCoreExceptionMapper implements ExceptionMapper<Throwable> {

    SampleCoreExceptionMapper() { }

    @Override
    public Response toResponse(final Throwable exception) {
        return Response.serverError().build();
    }
}
