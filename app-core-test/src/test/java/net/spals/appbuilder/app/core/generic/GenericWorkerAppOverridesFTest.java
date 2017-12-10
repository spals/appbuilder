package net.spals.appbuilder.app.core.generic;

import com.google.common.collect.ImmutableSet;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.config.service.ServiceScan;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Functional tests for binding overrides within a {@link GenericWorkerApp}
 *
 * @author tkral
 */
public class GenericWorkerAppOverridesFTest {
    private final Logger LOGGER = LoggerFactory.getLogger(GenericWorkerAppOverridesFTest.class);

    @Test
    public void testBindingOverrideError() {
        // This doesn't really test anything in the AppBuilder code
        // but just proves out how Guice errors out on override bindings
        catchException(() -> new GenericWorkerApp.Builder("testModuleOverrideError", LOGGER)
            .addModule(binder -> binder.bind(MyCoreSingletonService.class)
                .toInstance(new MyCoreSingletonServiceA()))
            .addModule(binder -> binder.bind(MyCoreSingletonService.class)
                .toInstance(new MyCoreSingletonServiceB()))
            .build());

        assertThat(caughtException(), instanceOf(CreationException.class));
    }

    @DataProvider
    Object[][] bindingOverridesProvider() {
        final MyCoreSingletonService serviceA = new MyCoreSingletonServiceA();
        final MyCoreSingletonService serviceB = new MyCoreSingletonServiceB();

        return new Object[][] {
            // Ensure that binding order is honored. The last one in is chosen
            {serviceA, serviceB},
            {serviceB, serviceA},
        };
    }

    @Test(dataProvider = "bindingOverridesProvider")
    public void testBindingOverrides(final MyCoreSingletonService firstService,
                                     final MyCoreSingletonService secondService) {
        final GenericWorkerApp app = new GenericWorkerApp.Builder(
                "testEnableBindingOverrides", LOGGER)
            .addModule(binder -> binder.bind(MyCoreSingletonService.class)
                .toInstance(firstService))
            .addModule(binder -> binder.bind(MyCoreSingletonService.class)
                .toInstance(secondService))
            .enableBindingOverrides()
            .build();

        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(MyCoreSingletonService.class), is(secondService));
    }

    @Test
    public void testCustomBindingOverridesAutoBind() {
        final ServiceScan serviceScan = createServiceScan();

        final GenericWorkerApp app = new GenericWorkerApp.Builder(
                "testEnableBindingOverrides", LOGGER)
            .setServiceScan(serviceScan)
            .addModule(binder -> binder.bind(MyCoreSingletonService.class)
                .toInstance(new MyCoreSingletonServiceA()))
            .enableBindingOverrides()
            .build();

        // Assert that the binding override uses the service from the custom module,
        // not the auto-bound service because the custom module came second.
        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(MyCoreSingletonService.class),
            instanceOf(MyCoreSingletonServiceA.class));
    }

    @Test
    public void testCustomBindingOverridesAutoBind2() {
        final ServiceScan serviceScan = createServiceScan();

        final GenericWorkerApp app = new GenericWorkerApp.Builder(
                "testEnableBindingOverrides", LOGGER)
            .addModule(binder -> binder.bind(MyCoreSingletonService.class)
                .toInstance(new MyCoreSingletonServiceA()))
            .setServiceScan(serviceScan)
            .addModule(binder -> binder.bind(MyCoreSingletonService.class)
                .toInstance(new MyCoreSingletonServiceB()))
            .enableBindingOverrides()
            .build();

        // Assert that the binding override uses the service from the second custom module.
        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(MyCoreSingletonService.class),
                instanceOf(MyCoreSingletonServiceB.class));
    }

    @Test
    public void testAutoBindOverridesCustomBinding() {
        final ServiceScan serviceScan = createServiceScan();

        final GenericWorkerApp app = new GenericWorkerApp.Builder(
                "testEnableBindingOverrides", LOGGER)
            .addModule(binder -> binder.bind(MyCoreSingletonService.class)
                .toInstance(new MyCoreSingletonServiceA()))
            .setServiceScan(serviceScan)
            .enableBindingOverrides()
            .build();

        // Assert that the binding override uses the auto-bound service,
        // not the custom module service because the auto-binding came second.
        final Injector serviceInjector = app.getServiceInjector();
        assertThat(serviceInjector.getInstance(MyCoreSingletonService.class),
            instanceOf(MyCoreSingleTonServiceAutoBind.class));
    }

    private ServiceScan createServiceScan() {
        final Reflections reflections = mock(Reflections.class);
        when(reflections.getTypesAnnotatedWith(eq(AutoBindSingleton.class)))
            .thenReturn(ImmutableSet.of(MyCoreSingleTonServiceAutoBind.class));
        return new ServiceScan.Builder().setReflections(reflections).build();
    }

    private interface MyCoreSingletonService {  }

    private class MyCoreSingletonServiceA implements MyCoreSingletonService {  }

    private class MyCoreSingletonServiceB implements MyCoreSingletonService {  }

    @AutoBindSingleton(baseClass = MyCoreSingletonService.class)
    private static class MyCoreSingleTonServiceAutoBind implements MyCoreSingletonService {  }
}
