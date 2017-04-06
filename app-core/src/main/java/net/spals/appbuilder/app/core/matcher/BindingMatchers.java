package net.spals.appbuilder.app.core.matcher;

import com.google.inject.Binding;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;

import java.io.Serializable;

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
}
