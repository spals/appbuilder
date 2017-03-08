package net.spals.appbuilder.app.core.grapher.ascii;

import com.github.mdr.ascii.graph.Graph;
import com.github.mdr.ascii.java.GraphBuilder;
import com.github.mdr.ascii.java.GraphLayouter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.grapher.*;
import net.spals.appbuilder.app.core.grapher.ServiceGrapher;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Stream;

/**
 * @author tkral
 */
public class AsciiServiceGrapher extends AbstractInjectorGrapher implements ServiceGrapher {

    private final Multimap<Vertex, Vertex> outboundEdges = HashMultimap.create();
    private final Multimap<Vertex, Vertex> inboundEdges = HashMultimap.create();

    private final HashSet<Vertex> allVertices = new HashSet<>();

    private final Logger logger;

    public AsciiServiceGrapher(final Logger logger) {
        this.logger = logger;
    }

    @VisibleForTesting
    boolean isOrphan(final Vertex vertex) {
        // An orphan is one without any outbound or inbound edges
        return outboundEdges.get(vertex).isEmpty() &&
                inboundEdges.get(vertex).isEmpty();
    }

    // ========== ServiceGrapher ==========

    @Override
    public synchronized AsciiServiceGrapher addEdge(final Vertex fromVertex, final Vertex toVertex) {
        outboundEdges.put(fromVertex, toVertex);
        inboundEdges.put(toVertex, fromVertex);
        return this;
    }

    @Override
    public AsciiServiceGrapher addVertex(final Vertex vertex) {
        allVertices.add(vertex);
        return this;
    }

    @Override
    public Type getType() {
        return Type.ASCII;
    }

    // ========== AbstractInjectorGrapher ==========

    @Override
    protected void reset() throws IOException {  }

    @Override
    protected void newInterfaceNode(final InterfaceNode node) throws IOException {
        logger.info("INTERFACE NODE: {}", new AsciiVertex(new Vertex.Builder().setNode(node).build()));
    }

    @Override
    protected void newImplementationNode(final ImplementationNode node) throws IOException {
        logger.info("IMPLEMENTATION NODE: {}", new AsciiVertex(new Vertex.Builder().setNode(node).build()));
    }

    @Override
    protected void newInstanceNode(final InstanceNode node) throws IOException {
        logger.info("INSTANCE NODE: {}", new AsciiVertex(new Vertex.Builder().setNode(node).build()));
    }

    @Override
    protected void newDependencyEdge(final DependencyEdge edge) throws IOException {

    }

    @Override
    protected void newBindingEdge(final BindingEdge edge) throws IOException {

    }

    @Override
    protected void postProcess() throws IOException {
        final GraphBuilder<AsciiVertex> asciiGraphBuilder = new GraphBuilder<>();
        // 1. Add all the edges to the graph builder
        final Stream<SimpleImmutableEntry<Vertex, Vertex>> singleEdges = outboundEdges.asMap().entrySet().stream()
                .flatMap(multiEdgeEntry -> {
                    final Stream<SimpleImmutableEntry<Vertex, Vertex>> edges =
                        multiEdgeEntry.getValue().stream()
                                .map(toVertex -> new SimpleImmutableEntry(multiEdgeEntry.getKey(), toVertex));
                    return edges;
                });
        singleEdges.forEach(singleEdgeEntry ->
                asciiGraphBuilder.addEdge(new AsciiVertex(singleEdgeEntry.getKey()), new AsciiVertex(singleEdgeEntry.getValue())));
        // 2. Add all orphaned vertices.
        allVertices.stream().filter(vertex -> isOrphan(vertex))
                .forEach(orphanVertex -> asciiGraphBuilder.addVertex(new AsciiVertex(orphanVertex)));

        final Graph<AsciiVertex> asciiGraph = asciiGraphBuilder.build();
        final GraphLayouter<AsciiVertex> asciiGraphLayouter = new GraphLayouter<>();
        asciiGraphLayouter.setVertical(false);

        logger.info(String.format("%n%s", asciiGraphLayouter.layout(asciiGraph)));
    }
}
