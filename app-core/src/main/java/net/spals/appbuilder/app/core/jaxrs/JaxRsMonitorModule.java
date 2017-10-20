package net.spals.appbuilder.app.core.jaxrs;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.ProvisionListener;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import net.spals.appbuilder.app.core.monitor.ParamSpanDecorator;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Configurable;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.BindingMatchers.keyTypeThat;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class JaxRsMonitorModule extends AbstractModule
    implements ProvisionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsMonitorModule.class);

    public abstract Configurable<?> getConfigurable();

    public static class Builder extends JaxRsMonitorModule_Builder {  }

    @Override
    protected void configure() {
        final Matcher bindingMatcher = keyTypeThat(rawTypeThat(subclassesOf(Tracer.class)));
        bindListener(bindingMatcher, this);
    }

    @Override
    public <T> void onProvision(final ProvisionInvocation<T> provision) {
        final Object monitorComponent = provision.provision();

        if (Tracer.class.isAssignableFrom(monitorComponent.getClass())) {
            registerTracer((Tracer)monitorComponent);
        }
    }

    void registerTracer(final Tracer tracer) {
        LOGGER.info("Enabling JaxRs server request tracing with OpenTracing");
        final ServerTracingDynamicFeature serverTracing =
                new ServerTracingDynamicFeature.Builder(tracer)
                        .withDecorators(ImmutableList.of(ServerSpanDecorator.STANDARD_TAGS,
                                ServerSpanDecorator.HTTP_WILDCARD_PATH_OPERATION_NAME,
                                new ParamSpanDecorator()))
                        .build();
        getConfigurable().register(serverTracing);
    }
}
