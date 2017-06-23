package net.spals.appbuilder.app.core.sample.web;

import net.spals.appbuilder.annotations.service.AutoBindSingleton;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

/**
 * A sample web request filter.
 *
 * @author tkral
 */
@AutoBindSingleton
public class SampleCoreRequestFilter implements ContainerRequestFilter {

    SampleCoreRequestFilter() { }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException { }
}
