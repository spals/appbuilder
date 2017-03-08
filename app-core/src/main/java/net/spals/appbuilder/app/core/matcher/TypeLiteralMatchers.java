package net.spals.appbuilder.app.core.matcher;

import com.google.common.base.Preconditions;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matcher implementations for use with {@link TypeLiteral}.
 *
 * @author tkral
 */
public class TypeLiteralMatchers {

    private TypeLiteralMatchers() {  }

    public static Matcher<TypeLiteral<?>> annotatedWith(
            final Class<? extends Annotation> annotationType) {
        return new AnnotatedWithType(annotationType);
    }

    private static class AnnotatedWithType extends AbstractMatcher<TypeLiteral<?>>
            implements Serializable {
        private final Class<? extends Annotation> annotationType;

        public AnnotatedWithType(final Class<? extends Annotation> annotationType) {
            this.annotationType = Preconditions.checkNotNull(annotationType, "annotation type");
            checkForRuntimeRetention(annotationType);
        }

        @Override
        public boolean matches(final TypeLiteral<?> typeLiteral) {
            return typeLiteral.getRawType().isAnnotationPresent(annotationType);
        }

        // Copied from: Matchers.checkForRuntimeRetention
        private static void checkForRuntimeRetention(final Class<? extends Annotation> annotationType) {
            final Retention retention = annotationType.getAnnotation(Retention.class);
            Preconditions.checkArgument(retention != null && retention.value() == RetentionPolicy.RUNTIME,
                    "Annotation " + annotationType.getSimpleName() + " is missing RUNTIME retention");
        }
    }

    public static Matcher<TypeLiteral<?>> any() {
        return ANY;
    }

    private static final Matcher<TypeLiteral<?>> ANY = new Any();

    private static class Any extends AbstractMatcher<TypeLiteral<?>>
            implements Serializable {

        @Override
        public boolean matches(final TypeLiteral<?> typeLiteral) {
            return true;
        }
    }

    public static Matcher<TypeLiteral<?>> none() {
        return new Not(ANY);
    }

    public static Matcher<TypeLiteral<?>> not(final Matcher<TypeLiteral<?>> matcher) {
        return new Not(matcher);
    }

    private static class Not extends AbstractMatcher<TypeLiteral<?>>
            implements Serializable {
        final Matcher<TypeLiteral<?>> delegate;

        private Not(Matcher<TypeLiteral<?>> delegate) {
            this.delegate = checkNotNull(delegate, "delegate");
        }

        @Override
        public boolean matches(final TypeLiteral<?> typeLiteral) {
            return !delegate.matches(typeLiteral);
        }
    }

    public static Matcher<TypeLiteral<?>> or(final Matcher<TypeLiteral<?>>... matchers) {
        return new Or(matchers);
    }

    private static class Or extends AbstractMatcher<TypeLiteral<?>>
            implements Serializable {
        final Matcher<TypeLiteral<?>>[] delegates;

        private Or(final Matcher<TypeLiteral<?>>... delegates) {
            this.delegates = delegates;
        }

        @Override
        public boolean matches(final TypeLiteral<?> typeLiteral) {
            return Arrays.asList(delegates).stream()
                    .anyMatch(delegate -> delegate.matches(typeLiteral));
        }
    }

    public static Matcher<TypeLiteral<?>> subclassesOf(final Class<?> superclass) {
        return new SubclassesOf(superclass);
    }

    private static class SubclassesOf extends AbstractMatcher<TypeLiteral<?>>
            implements Serializable {
        private final Class<?> superclass;

        public SubclassesOf(final Class<?> superclass) {
            this.superclass = Preconditions.checkNotNull(superclass, "superclass");
        }

        @Override
        public boolean matches(final TypeLiteral<?> typeLiteral) {
            return superclass.isAssignableFrom(typeLiteral.getRawType());
        }
    }
}
