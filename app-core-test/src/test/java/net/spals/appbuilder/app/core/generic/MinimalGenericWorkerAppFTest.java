package net.spals.appbuilder.app.core.generic;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import javax.ws.rs.core.Configurable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Functional tests for a minimal {@link GenericWorkerApp}
 *
 * @author tkral
 */
public class MinimalGenericWorkerAppFTest {
    private final Logger LOGGER = LoggerFactory.getLogger(MinimalGenericWorkerAppFTest.class);

    private final GenericWorkerApp minimalApp = new GenericWorkerApp.Builder("minimal", LOGGER).build();

    @Test
    public void testGenericWorkerAppLogger() {
        assertThat(minimalApp.getLogger(), sameInstance(LOGGER));
    }

    @Test
    public void testGenericWorkerAppName() {
        assertThat(minimalApp.getName(), is("minimal"));
    }

    @Test
    public void testMinimalServiceConfig() {
        assertThat(minimalApp.getServiceConfig(), is(ConfigFactory.empty()));
    }
}
