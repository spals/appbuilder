package net.spals.appbuilder.app.core.matcher;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Binding;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.ElementSource;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Matcher implementations for use with {@link Binding}.
 *
 * @author tkral
 */
public class BindingMatchers {

    private BindingMatchers() {  }

    public static Matcher<Binding<?>> any() {
        return ANY;
    }

    private static final Matcher<Binding<?>> ANY = new BindingMatchers.Any();

    private static class Any extends AbstractMatcher<Binding<?>>
            implements Serializable {

        @Override
        public boolean matches(final Binding<?> binding) {
            return true;
        }
    }

    public static <T extends Binding<T>> Matcher<Binding<?>> instanceOf(final Class<T> bindingType) {
        return new InstanceOf(bindingType);
    }

    private static class InstanceOf extends AbstractMatcher<Binding<?>>
            implements Serializable {
        private final Class<? extends Binding<?>> bindingType;

        private InstanceOf(final Class<? extends Binding<?>> bindingType) {
            this.bindingType = bindingType;
        }

        @Override
        public boolean matches(final Binding<?> binding) {
            return this.bindingType.isAssignableFrom(binding.getClass());
        }
    }

    public static Matcher<Binding<?>> withKeyTypeSubclassOf(final Class<?> keyTypeSuperClass) {
        return new WithKeyTypeSubclassOf(keyTypeSuperClass);
    }

    private static class WithKeyTypeSubclassOf extends AbstractMatcher<Binding<?>>
            implements Serializable {
        private final Matcher<TypeLiteral<?>> subclassOfMatcher;

        private WithKeyTypeSubclassOf(final Class<?> keyTypeSuperClass) {
            this.subclassOfMatcher = TypeLiteralMatchers.subclassesOf(keyTypeSuperClass);
        }

        @Override
        public boolean matches(final Binding<?> binding) {
            return subclassOfMatcher.matches(binding.getKey().getTypeLiteral());
        }
    }
    public static Matcher<Binding<?>> withSourcePackage(final String sourcePackage) {
        return new WithSourcePackage(sourcePackage);
    }

    private static class WithSourcePackage extends AbstractMatcher<Binding<?>>
            implements Serializable {
        private final String sourcePackage;

        private WithSourcePackage(final String sourcePackage) {
            this.sourcePackage = sourcePackage;
        }

        @Override
        public boolean matches(final Binding<?> binding) {
            return Optional.of(binding)
                    .flatMap(b -> parseSourceName(binding))
                    .map(bindingSource -> bindingSource.startsWith(this.sourcePackage))
                    .orElse(false);
        }

        @VisibleForTesting
        Optional<String> parseSourceName(final Binding<?> binding) {
            return Optional.ofNullable(binding.getSource()).map(source -> {
                if (source instanceof Class) {
                    return ((Class) source).getName();
                }

                return source.toString();
            });
        }
    }
}
