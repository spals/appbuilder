package net.spals.appbuilder.app.core.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinderBinding;
import com.google.inject.multibindings.MultibinderBinding;
import com.google.inject.multibindings.MultibindingsTargetVisitor;
import com.google.inject.multibindings.OptionalBinderBinding;
import com.google.inject.spi.*;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import net.spals.appbuilder.app.core.matcher.BindingMatchers;
import net.spals.appbuilder.graph.ServiceGrapher;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.writer.ServiceGraphWriter;
import net.spals.appbuilder.graph.writer.ServiceGraphWriterProvider;
import org.inferred.freebuilder.FreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class AutoBindServiceGrapherBootstrapModule implements BootstrapModule/*, InjectionListener<Object>, TypeListener*/ {

    public abstract String getFileName();
    public abstract Logger getLogger();

    public abstract ServiceGraphFormat getGraphFormat();
    public abstract ServiceGraph getServiceGraph();

    public static class Builder extends AutoBindServiceGrapherBootstrapModule_Builder {
        public Builder() {
            setGraphFormat(ServiceGraphFormat.NONE);
            setServiceGraph(new ServiceGraph());
        }
    }

    @Override
    public void configure(final BootstrapBinder bootstrapBinder) {
        final ServiceGraphWriterProvider graphWriterProvider =
                new ServiceGraphWriterProvider(getFileName(), getLogger(), getGraphFormat());
        final ServiceGraphWriter graphWriter = graphWriterProvider.get();

        final DefaultServiceGrapher2 serviceGrapher = new DefaultServiceGrapher2(graphWriter, getServiceGraph());
        bootstrapBinder.bindListener(BindingMatchers.any(), serviceGrapher);
//        bootstrapBinder.bind(ServiceGrapher.class).toInstance(new DefaultServiceGrapher(graphWriter, getServiceGraph()));
        bootstrapBinder.bind(ServiceGrapher.class).toInstance(serviceGrapher);
    }

    static class DefaultServiceGrapher2 implements ProvisionListener, ServiceGrapher {
        private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServiceGrapher2.class);

        private final ServiceGraphWriter graphWriter;
        private final ServiceGraph serviceGraph;

        DefaultServiceGrapher2(final ServiceGraphWriter graphWriter,
                               final ServiceGraph serviceGraph) {
            this.graphWriter = graphWriter;
            this.serviceGraph = serviceGraph;
        }

        @Override
        public <T> void onProvision(final ProvisionInvocation<T> provision) {
            LOGGER.info("PROVISION: {}", provision.getBinding());
        }

        @Override
        public void graph(final Injector injector) {

        }
    }

    static class DefaultServiceGrapher
            implements MultibindingsTargetVisitor<Object, Void>, ServiceGrapher {
        private static final Logger LOGGER = LoggerFactory.getLogger(DefaultServiceGrapher.class);

        private final ServiceGraphWriter graphWriter;
        private final ServiceGraph serviceGraph;

        DefaultServiceGrapher(final ServiceGraphWriter graphWriter,
                              final ServiceGraph serviceGraph) {
            this.graphWriter = graphWriter;
            this.serviceGraph = serviceGraph;
        }

        @Override
        public void graph(final Injector injector) {
            injector.getBindings().values().stream()
//                    .filter(binding -> isAppBuilderElement(binding))
                    .forEach(binding -> binding.acceptTargetVisitor(this));
            graphWriter.writeGraph(serviceGraph);
        }

        @VisibleForTesting
        Optional<String> elementModuleSource(final Element element) {
            if (element.getSource() instanceof String) {
                return Optional.ofNullable((String) element.getSource());
            } else if (element.getSource() instanceof ElementSource) {
                final ElementSource elementSource = (ElementSource) element.getSource();
                return Optional.ofNullable(elementSource.getModuleClassNames())
                        .filter(moduleClassNames -> !moduleClassNames.isEmpty())
                        .map(moduleClassNames -> moduleClassNames.get(0));
            }

            return Optional.empty();
        }

        @VisibleForTesting
        boolean isAppBuilderElement(final Element element) {
            return elementModuleSource(element)
                    .filter(elementModuleSource ->
                            elementModuleSource.startsWith("com.google.inject.multibindings") ||
                            elementModuleSource.startsWith("net.spals.appbuilder"))
                    .isPresent();
        }

        @Override
        public Void visit(final InstanceBinding<?> binding) {
            LOGGER.info("({}) INSTANCE BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ProviderInstanceBinding<?> binding) {
            LOGGER.info("({}) PROVIDER INSTANCE BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ProviderKeyBinding<?> binding) {
            LOGGER.info("({}) PROVIDER KEY BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final LinkedKeyBinding<?> binding) {
            LOGGER.info("({}) LINKED KEY BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ExposedBinding<?> binding) {
            LOGGER.info("({}) EXPOSED BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final UntargettedBinding<?> binding) {
            LOGGER.info("({}) UNTARGETTED BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ConstructorBinding<?> binding) {
            LOGGER.info("({}) CONSTRUCTOR BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ConvertedConstantBinding<?> binding) {
            LOGGER.info("({}) CONVERTED CONSTANT BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final ProviderBinding<?> binding) {
            LOGGER.info("({}) PROVIDER BINDING: {}", elementModuleSource(binding), binding);
            return null;
        }

        @Override
        public Void visit(final MultibinderBinding<?> multibinderBinding) {
            LOGGER.info("SET BINDING: {}", multibinderBinding.getSetKey());
            multibinderBinding.getElements().forEach(setBinding -> {
                LOGGER.info("  SET ELEMENT: {}", setBinding);
            });
            return null;
        }

        @Override
        public Void visit(final MapBinderBinding<?> mapBinderBinding) {
            LOGGER.info("MAP BINDING: {}", mapBinderBinding.getMapKey());
            mapBinderBinding.getEntries().forEach(mapEntryBinding -> {
                LOGGER.info("  MAP ELEMENT: {}", mapEntryBinding);
            });
            return null;
        }

        @Override
        public Void visit(final OptionalBinderBinding<?> optionalBinderBinding) {
            return null;
        }
    }
}
