package net.spals.appbuilder.app.core.bootstrap;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapBinder;
import org.reflections.Reflections;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.googlecode.catchexception.CatchException.verifyException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AutoBindModulesBootstrapModule}
 *
 * @author tkral
 */
public class AutoBindModulesBootstrapModuleTest {

    @Test
    public void testAutoBindModules() {
        final Reflections serviceScan = mock(Reflections.class);
        when(serviceScan.getTypesAnnotatedWith(any(Class.class)))
                .thenReturn(ImmutableSet.of(MyModule.class));

        final AutoBindModulesBootstrapModule autoBindBootstrapModule = new AutoBindModulesBootstrapModule(serviceScan);

        final BootstrapBinder bootstrapBinder = mock(BootstrapBinder.class);
        autoBindBootstrapModule.autoBindModules(bootstrapBinder);
        verify(bootstrapBinder).include(MyModule.class);
    }

    @DataProvider
    Object[][] invalidModuleClassProvider() {
        return new Object[][] {
                // Case: Interfaces not allowed
                {Module.class},
                // Case: Non-Module classes not allowed
                {String.class},
        };
    }

    @Test(dataProvider = "invalidModuleClassProvider")
    public void testInvalidModules(final Class<?> invalidModuleClazz) {
        final AutoBindModulesBootstrapModule autoBindBootstrapModule = new AutoBindModulesBootstrapModule(mock(Reflections.class));
        verifyException(() -> autoBindBootstrapModule.validateModules(ImmutableSet.of(invalidModuleClazz)), IllegalStateException.class);
    }

    private class MyModule extends AbstractModule {
        @Override
        protected void configure() {  }
    }
}
