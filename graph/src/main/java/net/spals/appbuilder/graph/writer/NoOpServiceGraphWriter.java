package net.spals.appbuilder.graph.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tkral
 */
class NoOpServiceGraphWriter implements ServiceGraphWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpServiceGraphWriter.class);

    @Override
    public void writeServiceGraph() {
        LOGGER.info("Skipping service graph write...");
    }
}
