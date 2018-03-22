package net.spals.appbuilder.app.core.jaxrs;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import net.spals.appbuilder.graph.model.IServiceGraphVertex;
import net.spals.appbuilder.graph.model.ServiceDAGVertex;
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

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.annotatedWith;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;
import static net.spals.appbuilder.graph.model.ServiceGraphVertex.createGraphVertex;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class JaxRsWebServerModule extends AbstractModule implements InjectionListener<Object>, TypeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsWebServerModule.class);

    public abstract boolean isWebServerAutoBindingEnabled();
    public abstract Configurable<?> getConfigurable();
    public abstract ServiceGraph getServiceGraph();

    public static class Builder extends JaxRsWebServerModule_Builder {
        public Builder() {
            setWebServerAutoBindingEnabled(true);
        }
    }

    @Override
    protected void configure() {
        final Matcher typeMatcher = annotatedWith(Path.class)
                .or(annotatedWith(Provider.class))
                .or(rawTypeThat(subclassesOf(DynamicFeature.class)))
                .or(rawTypeThat(subclassesOf(ExceptionMapper.class)))
                .or(rawTypeThat(subclassesOf(ContainerRequestFilter.class)))
                .or(rawTypeThat(subclassesOf(ContainerResponseFilter.class)));
        bindListener(typeMatcher, this);
    }

    @Override
    public void afterInjection(final Object wsComponent) {
        LOGGER.info("Registering WebServer component: {}", wsComponent);
        getConfigurable().register(wsComponent);

        final Key<Object> wsKey = (Key<Object>) Key.get(TypeLiteral.get(wsComponent.getClass()));
        final IServiceGraphVertex<?> wsVertex = createGraphVertex(wsKey, wsComponent);
        getServiceGraph().addVertex(wsVertex);
        getServiceGraph().addEdge(wsVertex, theWebServerVertex);
    }

    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral,
                         final TypeEncounter<I> typeEncounter) {
        if (isWebServerAutoBindingEnabled()) {
            // Add a dummy JAXRS WEBSERVER vertex to the service graph to show how WebServer components
            // relate to one another
            if (!getServiceGraph().containsVertex(theWebServerVertex)) {
                getServiceGraph().addVertex(theWebServerVertex);
            }

            typeEncounter.register(this);
        }
    }

    private static JaxRsWebServerVertex theWebServerVertex = new JaxRsWebServerVertex();

    /**
     * Special {@link ServiceDAGVertex} instance which
     * represents a JaxRs web server.
     *
     * All auto-bound webserver components will have an outgoing
     * edge to this vertex in order to show a complete graph.
     *
     * @author tkral
     */
    static class JaxRsWebServerVertex implements IServiceGraphVertex<String> {

        @Override
        public Key<String> getGuiceKey() {
            return Key.get(String.class);
        }

        @Override
        public String getServiceInstance() {
            return "JAX-RS WEBSERVER";
        }

        @Override
        public String toString(final String separator) {
            return getServiceInstance();
        }
    }
}
