package net.spals.appbuilder.config.matcher;

import com.google.inject.Binding;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkNotNull;

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

    public static Matcher<Binding<?>> none() {
        return new Not(ANY);
    }

    public static <T extends Binding<T>> Matcher<Binding<?>> not(final Matcher<Binding<?>> matcher) {
        return new Not(matcher);
    }

    private static class Not extends AbstractMatcher<Binding<?>>
        implements Serializable {
        final Matcher<Binding<?>> delegate;

        private Not(Matcher<Binding<?>> delegate) {
            this.delegate = checkNotNull(delegate, "delegate");
        }

        @Override
        public boolean matches(final Binding<?> binding) {
            return !delegate.matches(binding);
        }
    }

    public static Matcher<Binding<?>> keyTypeInPackage(final String keyTypePackagePrefix) {
        return new KeyTypeInPackage(keyTypePackagePrefix);
    }

    private static class KeyTypeInPackage extends AbstractMatcher<Binding<?>>
        implements Serializable {
        private final Matcher<TypeLiteral<?>> inPackageMatcher;

        private KeyTypeInPackage(final String keyTypePackagePrefix) {
            this.inPackageMatcher = TypeLiteralMatchers.inPackage(keyTypePackagePrefix);
        }

        @Override
        public boolean matches(final Binding<?> binding) {
            return inPackageMatcher.matches(binding.getKey().getTypeLiteral());
        }
    }

    public static Matcher<Binding<?>> keyTypeSubclassOf(final Class<?> keyTypeSuperClass) {
        return new KeyTypeSubclassOf(keyTypeSuperClass);
    }

    private static class KeyTypeSubclassOf extends AbstractMatcher<Binding<?>>
        implements Serializable {
        private final Matcher<TypeLiteral<?>> subclassOfMatcher;

        private KeyTypeSubclassOf(final Class<?> keyTypeSuperClass) {
            this.subclassOfMatcher = TypeLiteralMatchers.subclassesOf(keyTypeSuperClass);
        }

        @Override
        public boolean matches(final Binding<?> binding) {
            return subclassOfMatcher.matches(binding.getKey().getTypeLiteral());
        }
    }
}
