package net.spals.appbuilder.app.core.sample.web;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

/**
 * A sample web response filter.
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleCoreResponseFilter implements ContainerResponseFilter {

    SampleCoreResponseFilter() { }

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext) throws IOException { }
}
