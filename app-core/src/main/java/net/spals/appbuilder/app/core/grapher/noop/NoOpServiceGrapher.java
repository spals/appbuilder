package net.spals.appbuilder.app.core.grapher.noop;

import com.google.inject.Injector;
import net.spals.appbuilder.app.core.grapher.ServiceGrapher;
import org.slf4j.Logger;

/**
 * @author tkral
 */
public class NoOpServiceGrapher implements ServiceGrapher {

    private final Logger logger;

    public NoOpServiceGrapher(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public Type getType() {
        return Type.NO_OP;
    }

    @Override
    public NoOpServiceGrapher addEdge(final Vertex fromVertex, final Vertex toVertex) {
        return this;
    }

    @Override
    public NoOpServiceGrapher addVertex(final Vertex vertex) {
        return this;
    }

    @Override
    public void graph(final Injector injector) {
        logger.info("Skipping service graph...");
    }
}
