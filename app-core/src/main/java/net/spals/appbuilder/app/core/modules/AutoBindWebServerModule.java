package net.spals.appbuilder.app.core.modules;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import net.spals.appbuilder.app.core.matcher.TypeLiteralMatchers;
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
public class AutoBindWebServerModule extends AbstractModule implements InjectionListener<Object>, TypeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindWebServerModule.class);

    private final Configurable<?> configurable;

    public AutoBindWebServerModule(final Configurable<?> configurable) {
        this.configurable = configurable;
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
        configurable.register(wsComponent);
    }

    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral,
                         final TypeEncounter<I> typeEncounter) {
        typeEncounter.register(this);
    }
}
