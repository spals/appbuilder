package net.spals.appbuilder.app.core.generic;

import com.google.common.collect.ImmutableSet;
import com.google.inject.CreationException;
import com.netflix.governator.annotations.Configuration;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.core.App;
import net.spals.appbuilder.config.service.ServiceScan;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Functional tests for dealing with service class initialization errors
 * within an {@link App}.
 *
 * @author tkral
 */
public class InitErrorGenericWorkerAppFTest {
    private final Logger LOGGER = LoggerFactory.getLogger(InitErrorGenericWorkerAppFTest.class);

    @Test
    public void testConfigInitError() {
        final ServiceScan configInitErrorScan = new ServiceScan.Builder()
            .setReflections(createReflections(ConfigInitErrorService.class))
            .build();

        catchException(() -> new GenericWorkerApp.Builder("configInitError", LOGGER)
            .setServiceScan(configInitErrorScan)
            .build());

        // Ensure that we don't get the native NPE, but rather the NPE is wrapped
        // in a Guice exception.
        assertThat(caughtException(), instanceOf(CreationException.class));
    }

    @Test
    public void testStaticInitError() {
        final ServiceScan staticInitErrorScan = new ServiceScan.Builder()
            .setReflections(createReflections(StaticInitErrorService.class))
            .build();

        catchException(() -> new GenericWorkerApp.Builder("staticInitError", LOGGER)
            .setServiceScan(staticInitErrorScan)
            .build());

        // Ensure that we don't get the native NPE, but rather the NPE is wrapped
        // in a Guice exception.
        assertThat(caughtException(), instanceOf(CreationException.class));
    }

    private Reflections createReflections(final Class<?> serviceClass) {
        final Reflections reflections = mock(Reflections.class);
        when(reflections.getTypesAnnotatedWith(eq(AutoBindSingleton.class)))
            .thenReturn(ImmutableSet.of(StaticInitErrorService.class));

        return reflections;
    }

    @AutoBindSingleton
    private static class ConfigInitErrorService {

        @Configuration("config.init.error")
        private volatile String configValue = null;
        private volatile int configValueLengthNPE = configValue.length();
    }

    @AutoBindSingleton
    private static class StaticInitErrorService {
        private static String staticValue = null;
        private static int staticValueLengthNPE = staticValue.length();
    }
}


