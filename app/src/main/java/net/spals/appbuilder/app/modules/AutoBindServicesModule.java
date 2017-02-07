package net.spals.appbuilder.app.modules;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.*;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.ServletScopes;
import jersey.repackaged.com.google.common.base.Throwables;
import net.spals.appbuilder.annotations.service.*;
import net.spals.appbuilder.annotations.service.AutoBindProvider.ProviderScope;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * @author tkral
 */
public class AutoBindServicesModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoBindServicesModule.class);

    private final Reflections serviceScan;

    public AutoBindServicesModule(final Reflections serviceScan) {
        this.serviceScan = serviceScan;
    }

    @Override
    protected void configure() {
        autoBindFactories(binder());
        autoBindMaps(binder());
        autoBindProviders(binder());
        autoBindSets(binder());
        autoBindSingletons(binder());
    }

    @VisibleForTesting
    void autoBindFactories(final Binder binder) {
        final Set<Class<?>> factoryClasses = serviceScan.getTypesAnnotatedWith(AutoBindFactory.class);
        validateFactories(factoryClasses);

        factoryClasses.forEach(factoryClazz -> {
            LOGGER.info("Binding @AutoBindFactory: {}", factoryClazz);
            binder.install(new FactoryModuleBuilder().build(factoryClazz));
        });
    }

    @VisibleForTesting
    void autoBindMaps(final Binder binder) {
        final Set<Class<?>> mapClasses = serviceScan.getTypesAnnotatedWith(AutoBindInMap.class);
        validateSingletons(mapClasses, AutoBindInMap.class);

        mapClasses.stream()
            .forEach(mapClazz -> {
                final AutoBindInMap autoBindInMap = mapClazz.getAnnotation(AutoBindInMap.class);
                checkState(autoBindInMap.keyType() == String.class || autoBindInMap.keyType().isEnum(),
                        "@AutoBindInMap.keyType must be String or an Enum");
                LOGGER.info("Binding @AutoBindInMap[{}:{}]: {}", new Object[] {autoBindInMap.key(),
                    autoBindInMap.baseClass().getSimpleName(), mapClazz});

                final MapBinder mapBinder = MapBinder.newMapBinder(binder,
                        autoBindInMap.keyType(), autoBindInMap.baseClass());
                final Object key = autoBindInMap.keyType() == String.class ? autoBindInMap.key() :
                        Enum.valueOf((Class) autoBindInMap.keyType(), autoBindInMap.key());
                mapBinder.addBinding(key).to((Class) mapClazz).asEagerSingleton();
            });
    }

    @VisibleForTesting
    void autoBindProviders(final Binder binder) {
        final Set<Class<?>> providerClasses = serviceScan.getTypesAnnotatedWith(AutoBindProvider.class);
        validateProviders(providerClasses);

        providerClasses.stream()
            .forEach(providerClazz -> {
                final AutoBindProvider autoBindProvider = providerClazz.getAnnotation(AutoBindProvider.class);
                LOGGER.info("Binding @AutoBindProvider: {}", providerClazz);

                // Taken from Governator's ProviderBinderUtil
                final Class<?> providedType;
                try {
                    providedType = providerClazz.getMethod("get").getReturnType();
                } catch (NoSuchMethodException e) {
                    throw Throwables.propagate(e);
                }

                binder.bind(providedType)
                        .toProvider((javax.inject.Provider) new AutoBoundProvider((Class<? extends javax.inject.Provider>) providerClazz))
                        .in(mapProviderScope(autoBindProvider.value()));
            });
    }

    @VisibleForTesting
    void autoBindSets(final Binder binder) {
        final Set<Class<?>> setClasses = serviceScan.getTypesAnnotatedWith(AutoBindInSet.class);
        validateSingletons(setClasses, AutoBindInSet.class);

        setClasses.stream()
            .forEach(setClazz -> {
                final AutoBindInSet autoBindInSet = setClazz.getAnnotation(AutoBindInSet.class);
                LOGGER.info("Binding @AutoBindInSet[{}]: {}", autoBindInSet.baseClass(), setClazz);

                final Multibinder multibinder = Multibinder.newSetBinder(binder, autoBindInSet.baseClass());
                multibinder.addBinding().to((Class) setClazz).asEagerSingleton();
            });
    }

    @VisibleForTesting
    void autoBindSingletons(final Binder binder) {
        final Set<Class<?>> singletonClasses = serviceScan.getTypesAnnotatedWith(AutoBindSingleton.class);
        validateSingletons(singletonClasses, AutoBindSingleton.class);

        singletonClasses.stream()
            .forEach(singletonClazz -> {
                final AutoBindSingleton autoBindSingleton = singletonClazz.getAnnotation(AutoBindSingleton.class);
                LOGGER.info("Binding @AutoBindSingleton: {}", singletonClazz);

                if (autoBindSingleton.baseClass() == Void.class) {
                    binder.bind(singletonClazz).to((Class) singletonClazz).asEagerSingleton();
                } else {
                    binder.bind(autoBindSingleton.baseClass()).to((Class) singletonClazz).asEagerSingleton();
                    if (autoBindSingleton.includeImpl()) {
                        binder.bind(singletonClazz).to((Class) singletonClazz).asEagerSingleton();
                    }
                }
            });
    }

    @VisibleForTesting
    Scope mapProviderScope(final ProviderScope providerScope) {
        switch (providerScope) {
            case NONE: return Scopes.NO_SCOPE;
            case REQUEST: return ServletScopes.REQUEST;
            case SESSION: return ServletScopes.SESSION;
            case SINGLETON:
            default:
                return Scopes.SINGLETON;
        }
    }

    @VisibleForTesting
    void validateFactories(final Set<Class<?>> factoryClasses) {
        final Set<Class<?>> invalidFactories = factoryClasses.stream()
                .filter(factoryClazz -> !factoryClazz.isInterface())
                .collect(Collectors.toSet());
        checkState(invalidFactories.isEmpty(),
                "@AutoBindFactory can only annotate interfaces: %s", invalidFactories);
    }

    @VisibleForTesting
    void validateProviders(final Set<Class<?>> providerClasses) {
        final Set<Class<?>> invalidProviders = providerClasses.stream()
                .filter(providerClazz -> providerClazz.isInterface()
                        || !javax.inject.Provider.class.isAssignableFrom(providerClazz))
                .collect(Collectors.toSet());
        checkState(invalidProviders.isEmpty(),
                "@AutoBindProvider can only annotate Provider classes: %s", invalidProviders);
    }

    @VisibleForTesting
    void validateSingletons(final Set<Class<?>> singletonClasses,
                            final Class<? extends Annotation> annotationClazz) {
        final Set<Class<?>> invalidSingletons = singletonClasses.stream()
                .filter(singletonClazz -> singletonClazz.isInterface()
                        || javax.inject.Provider.class.isAssignableFrom(singletonClazz))
                .collect(Collectors.toSet());
        checkState(invalidSingletons.isEmpty(),
                "@$s can only annotate non-Provider classes: %s", annotationClazz.getSimpleName(), invalidSingletons);
    }

    /**
     * A wrapper around a provider class annotated with @AutoBindProvider
     * which exposes it to the Guice injector.
     *
     * Taken from Governator's ProviderBinderUtil.
     *
     * @author tkral
     */
    @VisibleForTesting
    static class AutoBoundProvider implements com.google.inject.Provider {
        private final Class<? extends javax.inject.Provider> providerClazz;

        @Inject
        private Injector injector;

        @Inject
        public AutoBoundProvider(Class<? extends javax.inject.Provider> providerClazz) {
            this.providerClazz = providerClazz;
        }

        @Override
        public Object get() {
            javax.inject.Provider provider = injector.getInstance(providerClazz);
            return provider.get();
        }
    }
}
