package net.spals.appbuilder.app.core.jaxrs;

import com.google.common.base.Joiner;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.swagger.config.ScannerFactory;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.AcceptHeaderApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import net.spals.appbuilder.config.service.ServiceScan;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Configurable;

/**
 * A Guice {@link Module} which automatically creates
 * a documentation API.
 *
 * @author tkral
 */
@FreeBuilder
public abstract class JaxRsDocModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsDocModule.class);

    public abstract boolean isWebServerAutoBindingEnabled();
    public abstract String getApplicationName();
    public abstract Configurable<?> getConfigurable();
    public abstract ServiceScan getServiceScan();

    public static class Builder extends JaxRsDocModule_Builder {
        public Builder() {
            setWebServerAutoBindingEnabled(true);
        }
    }

    @Override
    protected void configure() {
        if (!isWebServerAutoBindingEnabled()) {
            return;
        }

        LOGGER.info("Registering documentation API via Swagger");
        // Automatically register Swagger API documentation endpoints
        getConfigurable().register(AcceptHeaderApiListingResource.class);
        getConfigurable().register(SwaggerSerializers.class);

        // Automatically create and register a Swagger scanner
        final BeanConfig swaggerConfig = new BeanConfig();
        swaggerConfig.setResourcePackage(Joiner.on(',').join(getServiceScan().getServicePackages()));
        swaggerConfig.setScan();
        swaggerConfig.setTitle(getApplicationName());

        ScannerFactory.setScanner(swaggerConfig);
    }
}
