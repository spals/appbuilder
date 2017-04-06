package net.spals.appbuilder.app.core.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.app.core.matcher.TypeLiteralMatchers;
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
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class AutoBindWebServerModule extends AbstractModule implements InjectionListener<Object>, TypeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindWebServerModule.class);

    public abstract Optional<Configurable<?>> getConfigurable();
    public abstract ServiceGraph getServiceGraph();

    public final boolean isActive() {
        return getConfigurable().isPresent();
    }

    public static class Builder extends AutoBindWebServerModule_Builder {  }

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
        checkState(getConfigurable().isPresent());

        LOGGER.info("Registering WebServer component: {}", wsComponent);
        getConfigurable().get().register(wsComponent);
    }

    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral,
                         final TypeEncounter<I> typeEncounter) {
        if (isActive()) {
            // Add a dummy WEBSERVER vertex to the service grapher to show how WebServer components
            // relate to one another
            final Key<WEBSERVER> wsKey = Key.get(WEBSERVER.class);
            final Key<I> wsComponentKey = Key.get(typeLiteral);
            getServiceGraph().addVertex(wsKey).addVertex(wsComponentKey).addEdge(wsComponentKey, wsKey);

            typeEncounter.register(this);
        }
    }

    private static class WEBSERVER { }
}
