package net.spals.appbuilder.app.core.modules;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.inject.*;
import com.google.inject.internal.BindingImpl;
import com.google.inject.matcher.Matcher;
import com.google.inject.multibindings.MapBinderBinding;
import com.google.inject.multibindings.MultibinderBinding;
import com.google.inject.multibindings.MultibindingsTargetVisitor;
import com.google.inject.multibindings.OptionalBinderBinding;
import com.google.inject.spi.*;
import net.spals.appbuilder.config.matcher.BindingMatchers;
import net.spals.appbuilder.config.service.ServiceScan;
import net.spals.appbuilder.graph.model.IServiceGraphVertex;
import net.spals.appbuilder.graph.model.ServiceGraph;
import net.spals.appbuilder.graph.model.ServiceGraphFormat;
import net.spals.appbuilder.graph.model.ServiceGraphs;
import net.spals.appbuilder.graph.writer.ServiceGraphWriter;
import org.inferred.freebuilder.FreeBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.inject.matcher.Matchers.subclassesOf;
import static net.spals.appbuilder.config.matcher.TypeLiteralMatchers.rawTypeThat;
import static net.spals.appbuilder.graph.model.ServiceGraphVertex.createGraphVertex;

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
        final ServiceInitErrorHandler serviceInitErrorHandler = new ServiceInitErrorHandler();

        // 1. Add a listener for all service graph provisioning (assuming that we actually want to show a graph).
        //    Note that this also encompasses the handler for service initialization errors.
        if (getGraphFormat() != ServiceGraphFormat.NONE) {
            final ServiceGraphBuilder serviceGraphBuilder =
                new ServiceGraphBuilder(getServiceGraph(), serviceInitErrorHandler, getServiceScan());
            binder().bindListener(BindingMatchers.any(), serviceGraphBuilder);
        } else {
            // In the cases where we don't show a graph, we'll still look for service initialization errors.
            binder().bindListener(BindingMatchers.any(), serviceInitErrorHandler);
        }

        // 2. Bind the serviceGraphWriter instance
        final ServiceGraphs serviceGraphs = new ServiceGraphs(getApplicationName(), getServiceGraph(),
            getServiceScan());
        binder().bind(ServiceGraphs.class).toInstance(serviceGraphs);
        final ServiceGraphWriter serviceGraphWriter =
            new ServiceGraphWriter(getGraphFormat(), serviceGraphs);
        binder().bind(ServiceGraphWriter.class).toInstance(serviceGraphWriter);
    }

    @VisibleForTesting
    static class ServiceGraphBuilder extends DefaultBindingTargetVisitor<Object, Void>
            implements MultibindingsTargetVisitor<Object, Void>, ProvisionListener {

        private final ServiceGraph serviceGraph;
        private final ServiceInitErrorHandler serviceInitErrorHandler;
        private final Matcher<Binding<?>> serviceScanMatcher;

        ServiceGraphBuilder(final ServiceGraph serviceGraph,
                            final ServiceInitErrorHandler serviceInitErrorHandler,
                            final ServiceScan serviceScan) {
            this.serviceGraph = serviceGraph;
            this.serviceInitErrorHandler = serviceInitErrorHandler;
            this.serviceScanMatcher = serviceScan.asBindingMatcher();
        }

        @Override
        public <T> void onProvision(final ProvisionInvocation<T> provision) {
            final T serviceInstance;
            try {
                serviceInstance = provision.provision();
            } catch (Throwable t) {
                // If we encounter any errors during provisioning, immediately stop the graph build
                throw serviceInitErrorHandler.createProvisionException(provision, t);
            }

            final IServiceGraphVertex<T> vertex =
                createGraphVertex(provision.getBinding().getKey(), serviceInstance);
            serviceGraph.addVertex(vertex);

            if (serviceScanMatcher.matches(provision.getBinding())) {
                provision.getBinding().acceptTargetVisitor(this);
            }
        }

        @Override
        public Void visit(final ConstructorBinding<?> binding) {
            final IServiceGraphVertex<?> bindingVertex = serviceGraph.findVertex(binding.getKey()).get();
            final Map<Dependency<?>, Optional<IServiceGraphVertex<?>>> dependencyVertices =
                binding.getConstructor().getDependencies().stream()
                    .collect(Collectors.toMap(Function.identity(),
                        dependency -> serviceGraph.findVertex(dependency.getKey())));

            dependencyVertices.forEach((dependency, vertex) -> {
                // If the dependency vertex already exists in the graph, then just add an edge between the two...
                if(vertex.isPresent()) {
                    serviceGraph.addEdge(vertex.get(), bindingVertex);
                // ...otherwise, we have a key mismatch. This happens when the dependency uses an interface
                // but the Guice binding uses the implementing singleton class. In that case, we'll create
                // a vertex ad-hoc here for the interface and provide the edge. This will be merged in the DAG.
                } else {
                    final Key<Object> dependencyKey = (Key<Object>)dependency.getKey();

                    final Injector injector = ((BindingImpl) binding).getInjector();
                    final Object dependencyInstance = injector.getInstance(dependencyKey);

                    final IServiceGraphVertex<?> dependencyVertex = createGraphVertex(dependencyKey, dependencyInstance);
                    serviceGraph.addVertex(dependencyVertex);
                    serviceGraph.addEdge(dependencyVertex, bindingVertex);
                }
            });
            return null;
        }

        @Override
        public Void visit(final MultibinderBinding<?> multibinding) {
            if (!multibinding.getElements().isEmpty()) {
                final IServiceGraphVertex<?> bindingVertex = serviceGraph.findVertex(multibinding.getSetKey()).get();
                final Binding<?> bindingElement = multibinding.getElements().get(0);

                final Set<IServiceGraphVertex<?>> dependencyVertices = serviceGraph.findAllVertices(
                    rawTypeThat(subclassesOf(bindingElement.getKey().getTypeLiteral().getRawType()))
                );

                dependencyVertices.forEach(dependencyVertex -> serviceGraph.addEdge(dependencyVertex, bindingVertex));
            }

            return null;
        }

        @Override
        public Void visit(final MapBinderBinding<?> mapbinding) {
            if (!mapbinding.getEntries().isEmpty()) {
                final IServiceGraphVertex<?> bindingVertex = serviceGraph.findVertex(mapbinding.getMapKey()).get();
                final Map.Entry<?, Binding<?>> bindingEntry = mapbinding.getEntries().get(0);

                final Set<IServiceGraphVertex<?>> dependencyVertices = serviceGraph.findAllVertices(
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

    @VisibleForTesting
    static class ServiceInitErrorHandler implements ProvisionListener {

        @Override
        public <T> void onProvision(final ProvisionInvocation<T> provision) {
            try {
                provision.provision();
            } catch (Throwable t) {
                throw createProvisionException(provision, t);
            }
        }

        public ProvisionException createProvisionException(
            final ProvisionInvocation<?> provision,
            final Throwable t
        ) {
            // On any unexpected provision exception, ensure that we get the full stack trace into the log.
            // We do this by separating each exception in the causal chain into a separate message. Guice,
            // by convention, will print the individual causes in a messages list. Unfortunately, we have to
            // do this because Guice will swallow stack traces for only a single exception.
            final List<Message> messages = Throwables.getCausalChain(t).stream()
                .map(cause -> new Message(
                    cause.getClass().getSimpleName() + " during provision of "
                        + provision.getBinding().getKey().getTypeLiteral(),
                    cause
                ))
                .collect(Collectors.toList());

            return new ProvisionException(messages);
        }
    }
}
