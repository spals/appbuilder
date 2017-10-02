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

    public static Matcher<Binding<?>> keyTypeThat(final Matcher<TypeLiteral<?>> typeMatcher) {
        return new KeyTypeThat(typeMatcher);
    }

    private static class KeyTypeThat extends AbstractMatcher<Binding<?>>
        implements Serializable {
        private final Matcher<TypeLiteral<?>> typeMatcher;

        private KeyTypeThat(final Matcher<TypeLiteral<?>> typeMatcher) {
            this.typeMatcher = typeMatcher;
        }

        @Override
        public boolean matches(final Binding<?> binding) {
            return typeMatcher.matches(binding.getKey().getTypeLiteral());
        }
    }
}
