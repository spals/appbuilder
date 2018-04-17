package net.spals.appbuilder.app.core.jaxrs;

import com.google.inject.AbstractModule;
import io.swagger.jaxrs.listing.AcceptHeaderApiListingResource;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.*;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.FeatureContext;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class JaxRsCorsModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsCorsModule.class);

    public abstract boolean isCorsEnabled();
    public abstract Configurable<?> getConfigurable();

    public static class Builder extends JaxRsCorsModule_Builder {
        public Builder() {
            setCorsEnabled(false);
        }
    }

    @Override
    protected void configure() {
        getConfigurable().register(new JaxRsCorsDynamicFeature(isCorsEnabled()));

        if (isCorsEnabled()) {
            LOGGER.info("CORS is enabled for all API endpoints");
        } else {
            LOGGER.info("CORS is NOT enabled for all API endpoints");
        }
    }

    /**
     * A Jax-Rs {@link DynamicFeature} which enables CORS.
     *
     * @author tkral
     */
    static class JaxRsCorsDynamicFeature implements DynamicFeature {

        private final boolean isCorsEnabled;

        JaxRsCorsDynamicFeature(final boolean isCorsEnabled) {
            this.isCorsEnabled = isCorsEnabled;
        }

        @Override
        public void configure(
            final ResourceInfo resourceInfo,
            final FeatureContext featureContext
        ) {
            // If CORS is enabled for the entire web application, then register
            // the CORS filter for everything.
            if (isCorsEnabled) {
                featureContext.register(JaxRsCorsFilter.class);
            // Otherwise, guarantee that CORS is always enabled for the Swagger endpoint.
            // This is required for the Swagger UI to work correctly.
            } else if (resourceInfo.getResourceClass().equals(AcceptHeaderApiListingResource.class)) {
                featureContext.register(JaxRsCorsFilter.class);
            }
        }
    }

    /**
     * A generic Jax-Rs filter which provides CORS support.
     *
     * @author tkral
     */
    public static class JaxRsCorsFilter implements ContainerResponseFilter {

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
}
