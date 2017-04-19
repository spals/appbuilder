package net.spals.appbuilder.app.core.modules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.*;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.servlet.ServletScopes;
import net.spals.appbuilder.annotations.service.AutoBindInMap;
import net.spals.appbuilder.annotations.service.AutoBindProvider;
import net.spals.appbuilder.annotations.service.AutoBindSingleton;
import net.spals.appbuilder.app.core.modules.AutoBindServicesModule.AutoBoundProvider;
import org.hamcrest.Matchers;
import org.reflections.Reflections;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static com.googlecode.catchexception.CatchException.verifyException;
import static net.spals.appbuilder.annotations.service.AutoBindProvider.ProviderScope.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Unit tests for {@link AutoBindServicesModule}.
 *
 * @author tkral
 */
public class AutoBindServicesModuleTest {

    @Test
    public void testAutoBindFactories() {
        final Reflections serviceScan = mock(Reflections.class);
        when(serviceScan.getTypesAnnotatedWith(any(Class.class)))
                .thenReturn(ImmutableSet.of(MyFactory.class));

        final AutoBindServicesModule autoBindModule = new AutoBindServicesModule.Builder()
                .setServiceScan(serviceScan).build();

        final Binder binder = mock(Binder.class);
        autoBindModule.autoBindFactories(binder);
        verify(binder).install(isA(Module.class));
    }

    @DataProvider
    Object[][] autoBindProvidersProvider() {
        return new Object[][] {
                {MyProviderDefaultScope.class, Scopes.SINGLETON},
                {MyProviderNoScope.class, Scopes.NO_SCOPE},
                {MyProviderRequestScope.class, ServletScopes.REQUEST},
                {MyProviderSessionScope.class, ServletScopes.SESSION},
                {MyProviderSingletonScope.class, Scopes.SINGLETON},
        };
    }

    @Test(dataProvider = "autoBindProvidersProvider")
    public void testAutoBindProviders(final Class<? extends Provider> providerClazz, final Scope expectedScope) {
        final Reflections serviceScan = mock(Reflections.class);
        when(serviceScan.getTypesAnnotatedWith(any(Class.class)))
                .thenReturn(ImmutableSet.of(providerClazz));

        final AutoBindServicesModule autoBindModule = new AutoBindServicesModule.Builder()
                .setServiceScan(serviceScan).build();

        final ScopedBindingBuilder scopedBindingBuilder = mock(ScopedBindingBuilder.class);
        final AnnotatedBindingBuilder annotatedBindingBuilder = mock(AnnotatedBindingBuilder.class);
        when(annotatedBindingBuilder.toProvider(any(javax.inject.Provider.class))).thenReturn(scopedBindingBuilder);
        final Binder binder = mock(Binder.class);
        when(binder.bind(any(Key.class))).thenReturn(annotatedBindingBuilder);

        autoBindModule.autoBindProviders(binder);
        verify(binder).bind(eq(Key.get(String.class)));
        verify(annotatedBindingBuilder).toProvider(argThat(Matchers.<javax.inject.Provider>instanceOf(AutoBoundProvider.class)));
        verify(scopedBindingBuilder).in(eq(expectedScope));
    }

    @Test
    public void testAutoBindSimpleSingleton() {
        final Reflections serviceScan = mock(Reflections.class);
        when(serviceScan.getTypesAnnotatedWith(any(Class.class)))
                .thenReturn(ImmutableSet.of(MySingletonImplBind.class));

        final AutoBindServicesModule autoBindModule = new AutoBindServicesModule.Builder()
                .setServiceScan(serviceScan).build();

        final AnnotatedBindingBuilder annotatedBindingBuilder = mock(AnnotatedBindingBuilder.class);
        final Binder binder = mock(Binder.class);
        when(binder.bind(any(Class.class))).thenReturn(annotatedBindingBuilder);

        autoBindModule.autoBindSingletons(binder);
        verify(binder).bind(MySingletonImplBind.class);
        verifyNoMoreInteractions(binder);
        verify(annotatedBindingBuilder).asEagerSingleton();
    }

    @Test
    public void testAutoBindSingletonWithInterface() {
        final Reflections serviceScan = mock(Reflections.class);
        when(serviceScan.getTypesAnnotatedWith(any(Class.class)))
                .thenReturn(ImmutableSet.of(MySingletonInterfaceBind.class));

        final AutoBindServicesModule autoBindModule = new AutoBindServicesModule.Builder()
                .setServiceScan(serviceScan).build();

        final ScopedBindingBuilder scopedBindingBuilder = mock(ScopedBindingBuilder.class);
        final AnnotatedBindingBuilder annotatedBindingBuilder = mock(AnnotatedBindingBuilder.class);
        when(annotatedBindingBuilder.to(any(Class.class))).thenReturn(scopedBindingBuilder);
        final Binder binder = mock(Binder.class);
        when(binder.bind(any(Class.class))).thenReturn(annotatedBindingBuilder);

        autoBindModule.autoBindSingletons(binder);
        verify(binder).bind(MySingleton.class);
        verifyNoMoreInteractions(binder);
        verify(annotatedBindingBuilder).to(eq(MySingletonInterfaceBind.class));
        verify(scopedBindingBuilder).asEagerSingleton();
    }

    @Test
    public void testAutoBindSingletonWithInterfaceAndImpl() {
        final Reflections serviceScan = mock(Reflections.class);
        when(serviceScan.getTypesAnnotatedWith(any(Class.class)))
                .thenReturn(ImmutableSet.of(MySingletonInterfaceAndImplBind.class));

        final AutoBindServicesModule autoBindModule = new AutoBindServicesModule.Builder()
                .setServiceScan(serviceScan).build();

        final ScopedBindingBuilder scopedBindingBuilder = mock(ScopedBindingBuilder.class);
        final AnnotatedBindingBuilder annotatedBindingBuilder = mock(AnnotatedBindingBuilder.class);
        when(annotatedBindingBuilder.to(any(Class.class))).thenReturn(scopedBindingBuilder);
        final Binder binder = mock(Binder.class);
        when(binder.bind(any(Class.class))).thenReturn(annotatedBindingBuilder);

        autoBindModule.autoBindSingletons(binder);
        verify(binder).bind(MySingleton.class);
        verify(binder).bind(MySingletonInterfaceAndImplBind.class);
        verifyNoMoreInteractions(binder);
        verify(annotatedBindingBuilder).to(eq(MySingletonInterfaceAndImplBind.class));
        verify(scopedBindingBuilder).asEagerSingleton();
    }

    @DataProvider
    Object[][] invalidFactoryClassProvider() {
        return new Object[][] {
                // Case: Classes not allowed
                {MySingletonImplBind.class},
        };
    }

    @Test(dataProvider = "invalidFactoryClassProvider")
    public void testInvalidFactories(final Class<?> invalidFactoryClazz) {
        final AutoBindServicesModule autoBindModule = new AutoBindServicesModule.Builder().build();
        verifyException(() -> autoBindModule.validateFactories(ImmutableSet.of(invalidFactoryClazz)), IllegalStateException.class);
    }

    @DataProvider
    Object[][] invalidProviderClassProvider() {
        return new Object[][] {
                // Case: Interfaces not allowed
                {Provider.class},
                // Case: Non-Provider classes not allowed
                {MySingletonImplBind.class},
        };
    }

    @Test(dataProvider = "invalidProviderClassProvider")
    public void testInvalidProviders(final Class<?> invalidProviderClazz) {
        final AutoBindServicesModule autoBindModule = new AutoBindServicesModule.Builder().build();
        verifyException(() -> autoBindModule.validateProviders(ImmutableSet.of(invalidProviderClazz)), IllegalStateException.class);
    }

    @DataProvider
    Object[][] invalidSingletonClassProvider() {
        return new Object[][] {
                // Case: Interfaces not allowed
                {MyFactory.class},
                // Case: Provider classes not allowed
                {MyProviderDefaultScope.class},
        };
    }

    @Test(dataProvider = "invalidSingletonClassProvider")
    public void testInvalidSingletons(final Class<?> invalidSingletonClazz) {
        final AutoBindServicesModule autoBindModule = new AutoBindServicesModule.Builder().build();
        verifyException(() -> autoBindModule.validateSingletons(ImmutableSet.of(invalidSingletonClazz), AutoBindSingleton.class),
                IllegalStateException.class);
    }

    private interface MyFactory {  }

    @AutoBindProvider
    private class MyProviderDefaultScope implements Provider<String> {
        @Override
        public String get() {
            return "";
        }
    }

    @AutoBindProvider(NONE)
    private class MyProviderNoScope implements Provider<String> {
        @Override
        public String get() {
            return "";
        }
    }

    @AutoBindProvider(REQUEST)
    private class MyProviderRequestScope implements Provider<String> {
        @Override
        public String get() {
            return "";
        }
    }

    @AutoBindProvider(SESSION)
    private class MyProviderSessionScope implements Provider<String> {
        @Override
        public String get() {
            return "";
        }
    }

    @AutoBindProvider(SINGLETON)
    private class MyProviderSingletonScope implements Provider<String> {
        @Override
        public String get() {
            return "";
        }
    }

    private interface MySingleton {  }

    @AutoBindInMap(baseClass = MySingleton.class, key = "singleton")
    private class MySingletonInMap implements MySingleton {  }

    @AutoBindSingleton
    private class MySingletonImplBind implements MySingleton {  }

    @AutoBindSingleton(baseClass = MySingleton.class)
    private class MySingletonInterfaceBind implements MySingleton {  }

    @AutoBindSingleton(baseClass = MySingleton.class, includeImpl = true)
    private class MySingletonInterfaceAndImplBind implements MySingleton {  }
}
