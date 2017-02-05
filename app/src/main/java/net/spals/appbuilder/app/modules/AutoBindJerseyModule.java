package net.spals.appbuilder.app.modules;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author tkral
 */
public class AutoBindJerseyModule extends AbstractModule implements InjectionListener<Object>, TypeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindJerseyModule.class);

    private final ResourceConfig jerseyConfig;

    public AutoBindJerseyModule(final ResourceConfig jerseyConfig) {
        this.jerseyConfig = jerseyConfig;
    }

    @Override
    protected void configure() {
        final Matcher typeMatcher = Matchers.annotatedWith(Path.class)
                .or(Matchers.annotatedWith(Provider.class))
                .or((Matcher) Matchers.subclassesOf(DynamicFeature.class))
                .or((Matcher) Matchers.subclassesOf(ExceptionMapper.class))
                .or((Matcher) Matchers.subclassesOf(ContainerRequestFilter.class))
                .or((Matcher) Matchers.subclassesOf(ContainerResponseFilter.class));
        bindListener(typeMatcher, this);
    }

    @Override
    public void afterInjection(final Object jerseyComponent) {
        LOGGER.info("Registering Jersey component: {}", jerseyComponent);
        jerseyConfig.register(jerseyComponent);
    }

    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral,
                         final TypeEncounter<I> typeEncounter) {
        typeEncounter.register(this);
    }
}
