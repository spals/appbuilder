package net.spals.appbuilder.app.core.jaxrs;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * A generic Jax-Rs filter which enables CORS.
 *
 * @author tkral
 */
public class JaxRsCorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(
        final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext
    ) {
        responseContext.getHeaders().add(
            "Access-Control-Allow-Origin", "*"
        );
        responseContext.getHeaders().add(
            "Access-Control-Allow-Credentials", "true"
        );
        responseContext.getHeaders().add(
            "Access-Control-Allow-Headers",
            "origin, content-type, accept, authorization"
        );
        responseContext.getHeaders().add(
            "Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS, HEAD"
        );
    }
}
