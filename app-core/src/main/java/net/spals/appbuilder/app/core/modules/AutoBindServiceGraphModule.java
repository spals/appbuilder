package net.spals.appbuilder.app.core.modules;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.matcher.Matcher;
import com.google.inject.multibindings.MapBinderBinding;
import com.google.inject.multibindings.MultibinderBinding;
import com.google.inject.multibindings.MultibindingsTargetVisitor;
import com.google.inject.multibindings.OptionalBinderBinding;
import com.google.inject.spi.*;
import net.spals.appbuilder.app.core.matcher.BindingMatchers;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.writer.ServiceGraphWriter;
import net.spals.appbuilder.graph.writer.ServiceGraphWriterProvider;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class AutoBindServiceGraphModule extends AbstractModule {

    public abstract ServiceGraphFormat getGraphFormat();
    public abstract ServiceGraph getServiceGraph();

    public static class Builder extends AutoBindServiceGraphModule_Builder {
        public Builder() {
            setGraphFormat(ServiceGraphFormat.NONE);
        }
    }

    @Override
    public void configure() {
        // 1. Add a listener for all service provisioning
        final ServiceGraphBindingTargetVisitor serviceGraphTargetVisitor =
                new ServiceGraphBindingTargetVisitor(getServiceGraph());
        final ServiceGraphProvisionListener serviceGraphProvisionListener =
                new ServiceGraphProvisionListener(serviceGraphTargetVisitor);
        binder().bindListener(BindingMatchers.any(), serviceGraphProvisionListener);

        // 2. Bind the serviceGraphWriter instance
        final ServiceGraphWriterProvider serviceGraphWriterProvider =
                new ServiceGraphWriterProvider(getServiceGraph(), getGraphFormat());
        binder().bind(ServiceGraphWriter.class).toProvider(serviceGraphWriterProvider).asEagerSingleton();
    }

    @VisibleForTesting
    static class ServiceGraphProvisionListener implements ProvisionListener {

        private final ServiceGraphBindingTargetVisitor targetVisitor;

        ServiceGraphProvisionListener(final ServiceGraphBindingTargetVisitor targetVisitor) {
            this.targetVisitor = targetVisitor;
        }

        @Override
        public <T> void onProvision(final ProvisionInvocation<T> provision) {
            provision.getBinding().acceptTargetVisitor(targetVisitor);
        }
    }

    @VisibleForTesting
    static class ServiceGraphBindingTargetVisitor extends DefaultBindingTargetVisitor<Object, Void>
            implements MultibindingsTargetVisitor<Object, Void> {
        private static final Logger LOGGER = LoggerFactory.getLogger(ServiceGraphBindingTargetVisitor.class);

        private static final Matcher<Binding<?>> APPBUILDER_MATCHER = BindingMatchers.withSourcePackage("net.spals.appbuilder");

        private final ServiceGraph serviceGraph;

        ServiceGraphBindingTargetVisitor(final ServiceGraph serviceGraph) {
            this.serviceGraph = serviceGraph;
        }

        @VisibleForTesting
        Optional<String> elementModuleSource(final Element element) {
            return Optional.ofNullable(element.getSource()).map(Objects::toString);
        }

        @Override
        public Void visit(final InstanceBinding<?> binding) {
            Optional.of(binding).filter(b -> APPBUILDER_MATCHER.matches(b))
                    .ifPresent(b -> serviceGraph.addVertex(b.getKey()));
            return null;
        }

        @Override
        public Void visit(final ProviderInstanceBinding<?> binding) {
            LOGGER.trace("({}) PROVIDER INSTANCE BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ProviderKeyBinding<?> binding) {
            LOGGER.trace("({}) PROVIDER KEY BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final LinkedKeyBinding<?> binding) {
            LOGGER.trace("({}) LINKED KEY BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ExposedBinding<?> binding) {
            LOGGER.trace("({}) EXPOSED BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final UntargettedBinding<?> binding) {
            LOGGER.trace("({}) UNTARGETTED BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ConstructorBinding<?> binding) {
            Optional.of(binding).filter(b -> APPBUILDER_MATCHER.matches(b))
                    .ifPresent(b -> {
                        serviceGraph.addVertex(b.getKey());
                        b.getConstructor().getDependencies().forEach(dependency ->
                                serviceGraph.addVertex(dependency.getKey()).addEdge(dependency.getKey(), b.getKey()));
                    });
            return null;
        }

        @Override
        public Void visit(final ConvertedConstantBinding<?> binding) {
            LOGGER.trace("({}) CONVERTED CONSTANT BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ProviderBinding<?> binding) {
            LOGGER.trace("({}) PROVIDER BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final MultibinderBinding<?> multibinderBinding) {
            LOGGER.trace("SET BINDING: {}", multibinderBinding.getSetKey());
            multibinderBinding.getElements().forEach(setBinding -> {
                LOGGER.trace("  SET ELEMENT: {}", setBinding);
            });
            return null;
        }

        @Override
        public Void visit(final MapBinderBinding<?> mapBinderBinding) {
            LOGGER.trace("MAP BINDING: {}", mapBinderBinding.getMapKey());
            mapBinderBinding.getEntries().forEach(mapEntryBinding -> {
                LOGGER.trace("  MAP ELEMENT: {}", mapEntryBinding);
            });
            return null;
        }

        @Override
        public Void visit(final OptionalBinderBinding<?> optionalBinderBinding) {
            return null;
        }
    }
}
