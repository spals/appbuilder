package net.spals.appbuilder.app.core.jaxrs;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import net.spals.appbuilder.config.matcher.TypeLiteralMatchers;
import net.spals.appbuilder.graph.model.ServiceGraph;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class JaxRsWebServerModule extends AbstractModule implements InjectionListener<Object>, TypeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsWebServerModule.class);

    public abstract boolean isActive();
    public abstract Configurable<?> getConfigurable();
    public abstract ServiceGraph getServiceGraph();

    public static class Builder extends JaxRsWebServerModule_Builder {
        public Builder() {
            setActive(true);
        }
    }

    @Override
    protected void configure() {
        final Matcher typeMatcher = TypeLiteralMatchers.annotatedWith(Path.class)
                .or(TypeLiteralMatchers.annotatedWith(Provider.class))
                .or(TypeLiteralMatchers.subclassesOf(DynamicFeature.class))
                .or(TypeLiteralMatchers.subclassesOf(ExceptionMapper.class))
                .or(TypeLiteralMatchers.subclassesOf(ContainerRequestFilter.class))
                .or(TypeLiteralMatchers.subclassesOf(ContainerResponseFilter.class));
        bindListener(typeMatcher, this);
    }

    @Override
    public void afterInjection(final Object wsComponent) {
        LOGGER.info("Registering WebServer component: {}", wsComponent);
        getConfigurable().register(wsComponent);
    }

    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral,
                         final TypeEncounter<I> typeEncounter) {
        if (isActive()) {
            // Add a dummy WEBSERVER vertex to the service grapher to show how WebServer components
            // relate to one another
//            final Key<WEBSERVER> wsKey = Key.get(WEBSERVER.class);
//            final Key<I> wsComponentKey = Key.get(typeLiteral);
//            getServiceGraph().addVertex(wsKey).addVertex(wsComponentKey).addEdge(wsComponentKey, wsKey);

            typeEncounter.register(this);
        }
    }

    private static WEBSERVER theWebServer = new WEBSERVER();
    private static class WEBSERVER { }
}
