package net.spals.appbuilder.app.core.jaxrs;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import net.spals.appbuilder.config.matcher.TypeLiteralMatchers;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphVertex;
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
import java.lang.reflect.Type;

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

        final Key<Object> wsKey = (Key<Object>) Key.get(TypeLiteral.get(wsComponent.getClass()));
        final ServiceGraphVertex<?> wsVertex = ServiceGraphVertex.newVertex(wsKey, wsComponent);
        getServiceGraph().addVertex(wsVertex);
        getServiceGraph().addEdge(wsVertex, theWebServerVertex);
    }

    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral,
                         final TypeEncounter<I> typeEncounter) {
        if (isActive()) {
            // Add a dummy WEBSERVER vertex to the service grapher to show how WebServer components
            // relate to one another
            if (!getServiceGraph().containsVertex(theWebServerVertex)) {
                getServiceGraph().addVertex(theWebServerVertex);
            }

            typeEncounter.register(this);
        }
    }

    private static WebServerVertex theWebServerVertex = new WebServerVertex();

    /**
     * Special {@link ServiceGraphVertex} instance which
     * represents the application's web server.
     *
     * All auto-bound webserver components will have an outgoing
     * edge to this vertex in order to show a complete graph.
     *
     * @author tkral
     */
    static class WebServerVertex extends ServiceGraphVertex<String> {

        @Override
        public Key<String> getGuiceKey() {
            return Key.get(String.class);
        }

        @Override
        public String getServiceInstance() {
            return "WEBSERVER";
        }

        @Override
        public String toString() {
            return getServiceInstance();
        }
    }
}
