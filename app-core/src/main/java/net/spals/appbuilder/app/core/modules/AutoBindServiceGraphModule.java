package net.spals.appbuilder.app.core.modules;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.matcher.Matcher;
import com.google.inject.multibindings.MapBinderBinding;
import com.google.inject.multibindings.MultibinderBinding;
import com.google.inject.multibindings.MultibindingsTargetVisitor;
import com.google.inject.multibindings.OptionalBinderBinding;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.ProvisionListener;
import net.spals.appbuilder.config.matcher.BindingMatchers;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.model.ServiceGraphVertex;
import net.spals.appbuilder.graph.writer.ServiceGraphWriter;
import org.inferred.freebuilder.FreeBuilder;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;

/**
 * @author tkral
 */
@FreeBuilder
public abstract class AutoBindServiceGraphModule extends AbstractModule {

    public abstract String getApplicationName();
    public abstract ServiceGraphFormat getGraphFormat();
    public abstract ServiceGraph getServiceGraph();
    public abstract ServiceScan getServiceScan();

    public static class Builder extends AutoBindServiceGraphModule_Builder {
        public Builder(final String applicationName, final ServiceGraph serviceGraph) {
            setApplicationName(applicationName);
            setServiceGraph(serviceGraph);
            setGraphFormat(ServiceGraphFormat.NONE);
            setServiceScan(ServiceScan.empty());
        }
    }

    @Override
    public void configure() {
        // 1. Add a listener for all service provisioning
        //    (assuming that we actually want to show a graph).
        if (getGraphFormat() != ServiceGraphFormat.NONE) {
            final ServiceGraphListener serviceGraphListener =
                new ServiceGraphListener(getServiceGraph(), getServiceScan());
            binder().bindListener(BindingMatchers.any(), serviceGraphListener);
        }

        // 2. Bind the serviceGraphWriter instance
        final ServiceGraphWriter serviceGraphWriter =
            new ServiceGraphWriter(getApplicationName(), getGraphFormat(), getServiceScan());
        binder().bind(ServiceGraphWriter.class).toInstance(serviceGraphWriter);
    }

    @VisibleForTesting
    static class ServiceGraphListener extends DefaultBindingTargetVisitor<Object, Void>
            implements MultibindingsTargetVisitor<Object, Void>, ProvisionListener {

        private final ServiceGraph serviceGraph;
        private final Matcher<Binding<?>> serviceScanMatcher;

        ServiceGraphListener(final ServiceGraph serviceGraph,
                             final ServiceScan serviceScan) {
            this.serviceGraph = serviceGraph;
            this.serviceScanMatcher = serviceScan.asBindingMatcher();
        }

        @Override
        public <T> void onProvision(final ProvisionInvocation<T> provision) {
            final T serviceInstance = provision.provision();

            final ServiceGraphVertex<T> vertex =
                ServiceGraphVertex.newVertex(provision.getBinding().getKey(), serviceInstance);
            serviceGraph.addVertex(vertex);

            if (serviceScanMatcher.matches(provision.getBinding())) {
                provision.getBinding().acceptTargetVisitor(this);
            }
        }

        @Override
        public Void visit(final ConstructorBinding<?> binding) {
            final ServiceGraphVertex<?> bindingVertex = serviceGraph.findVertex(binding.getKey()).get();
            final Set<ServiceGraphVertex<?>> dependencyVertices =
                binding.getConstructor().getDependencies().stream()
                    .map(dependency -> serviceGraph.findVertex(dependency.getKey()))
                    .filter(vertexOpt -> vertexOpt.isPresent())
                    .map(vertexOpt -> vertexOpt.get())
                    .collect(Collectors.toSet());

            dependencyVertices.forEach(dependencyVertex -> serviceGraph.addEdge(dependencyVertex, bindingVertex));
            return null;
        }

        @Override
        public Void visit(final MultibinderBinding<?> multibinding) {
            if (!multibinding.getElements().isEmpty()) {
                final ServiceGraphVertex<?> bindingVertex = serviceGraph.findVertex(multibinding.getSetKey()).get();
                final Binding<?> bindingElement = multibinding.getElements().get(0);

                final Set<ServiceGraphVertex<?>> dependencyVertices = serviceGraph.findAllVertices(
                    rawTypeThat(subclassesOf(bindingElement.getKey().getTypeLiteral().getRawType()))
                );

                dependencyVertices.forEach(dependencyVertex -> serviceGraph.addEdge(dependencyVertex, bindingVertex));
            }

            return null;
        }

        @Override
        public Void visit(final MapBinderBinding<?> mapbinding) {
            if (!mapbinding.getEntries().isEmpty()) {
                final ServiceGraphVertex<?> bindingVertex = serviceGraph.findVertex(mapbinding.getMapKey()).get();
                final Map.Entry<?, Binding<?>> bindingEntry = mapbinding.getEntries().get(0);

                final Set<ServiceGraphVertex<?>> dependencyVertices = serviceGraph.findAllVertices(
                    rawTypeThat(subclassesOf(bindingEntry.getValue().getKey().getTypeLiteral().getRawType()))
                );

                dependencyVertices.forEach(dependencyVertex -> serviceGraph.addEdge(dependencyVertex, bindingVertex));
            }

            return null;
        }

        @Override
        public Void visit(final OptionalBinderBinding<?> optionalbinding) {
            return null;
        }
    }
}
