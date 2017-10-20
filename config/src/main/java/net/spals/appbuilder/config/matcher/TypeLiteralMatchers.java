package net.spals.appbuilder.config.matcher;

import com.google.common.base.Preconditions;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

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
        return typeLiteralThat(Matchers.any());
    }

    public static Matcher<TypeLiteral<?>> hasParameterTypeThat(final Matcher<TypeLiteral<?>> typeMatcher) {
        return new HasParameterTypeThat(typeMatcher);
    }

    private static class HasParameterTypeThat extends AbstractMatcher<TypeLiteral<?>>
        implements Serializable {
        private final Matcher<TypeLiteral<?>> typeMatcher;

        private HasParameterTypeThat(final Matcher<TypeLiteral<?>> typeMatcher) {
            this.typeMatcher = checkNotNull(typeMatcher, "typeMatcher");
        }

        @Override
        public boolean matches(final TypeLiteral<?> typeLiteral) {
            if (!(typeLiteral.getType() instanceof ParameterizedType)) {
                return false;
            }

            final ParameterizedType parameterizedType = (ParameterizedType) typeLiteral.getType();
            final List<Type> parameterTypes = Arrays.asList(parameterizedType.getActualTypeArguments());
            return parameterTypes.stream()
                .map(parameterType -> TypeLiteral.get(parameterType))
                .filter(parameterTypeLiteral -> typeMatcher.matches(parameterTypeLiteral))
                .findAny().isPresent();
        }
    }

    public static Matcher<TypeLiteral<?>> none() {
        return Matchers.not(any());
    }

    public static Matcher<TypeLiteral<?>> rawTypeThat(final Matcher<Class> rawTypeMatcher) {
        return new RawTypeThat(rawTypeMatcher);
    }

    private static class RawTypeThat extends AbstractMatcher<TypeLiteral<?>>
        implements Serializable {
        private final Matcher<Class> rawTypeMatcher;

        public RawTypeThat(final Matcher<Class> rawTypeMatcher) {
            this.rawTypeMatcher = rawTypeMatcher;
        }

        @Override
        public boolean matches(final TypeLiteral<?> typeLiteral) {
            return rawTypeMatcher.matches(typeLiteral.getRawType());
        }
    }

    public static Matcher<TypeLiteral<?>> typeLiteralThat(final Matcher<Object> objectMatcher) {
        return new TypeLiteralThat(objectMatcher);
    }

    private static class TypeLiteralThat extends AbstractMatcher<TypeLiteral<?>>
        implements Serializable {
        private final Matcher<Object> objectMatcher;

        public TypeLiteralThat(final Matcher<Object> objectMatcher) {
            this.objectMatcher = objectMatcher;
        }

        @Override
        public boolean matches(final TypeLiteral<?> typeLiteral) {
            return objectMatcher.matches(typeLiteral);
        }
    }
}

