package net.spals.appbuilder.config.matcher;

import com.google.inject.Binding;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

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
        return keyTypeThat(TypeLiteralMatchers.any());
    }

    public static Matcher<Binding<?>> none() {
        return Matchers.not(any());
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
