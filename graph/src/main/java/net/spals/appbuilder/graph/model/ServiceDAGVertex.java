package net.spals.appbuilder.graph.model;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import net.spals.appbuilder.config.TaggedConfig;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A definition of a vertex in the {@link ServiceDAG}.
 *
 * This is mostly used to convert information about
 * a service into an easy-to-understand string.
 *
 * All pieces of information are delimited by a
 * separator string. This class includes a default,
 * but you can overwrite the separator using
 * {@link PrintableVertex}.
 *
 * @author tkral
 */
@AutoValue
public abstract class ServiceDAGVertex<T> implements IServiceDAGVertex<T> {

    private final static String DEFAULT_SEPARATOR = " ";

    public static <T2> ServiceDAGVertex<T2> createVertex(final Key<T2> guiceKey, final T2 serviceInstance) {
        return new AutoValue_ServiceDAGVertex(guiceKey, serviceInstance, Optional.empty());
    }

    public static <T2> ServiceDAGVertex<T2> createVertexWithProvider(final IServiceDAGVertex<T2> vertex,
                                                                     final IServiceDAGVertex<?> providerSource) {
        return new AutoValue_ServiceDAGVertex(vertex.getGuiceKey(), vertex.getServiceInstance(),
            Optional.ofNullable(providerSource));
    }

    @Override
    public abstract Key<T> getGuiceKey();

    @Override
    public abstract T getServiceInstance();

    @Override
    public abstract Optional<IServiceDAGVertex<?>> getProviderSource();

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(IServiceDAGVertex.class.isAssignableFrom(obj.getClass()))) {
            return false;
        }
        final IServiceDAGVertex that = (IServiceDAGVertex) obj;
        return Objects.equal(getGuiceKey(), that.getGuiceKey()) &&
            Objects.equal(getServiceInstance(), that.getServiceInstance()) &&
            Objects.equal(getProviderSource(), that.getProviderSource());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getGuiceKey(), getServiceInstance(), getProviderSource());
    }

    @Override
    public String toString() {
        return toString(DEFAULT_SEPARATOR);
    }

    @Override
    public String toString(final String separator) {
        final Class<? extends Annotation> serviceAnnotationType = getGuiceKey().getAnnotationType();

        final StringBuilder sb = new StringBuilder();
        if (serviceAnnotationType != null) {
            sb.append(annotationTypeName(serviceAnnotationType, Optional.ofNullable(getGuiceKey().getAnnotation())))
                .append(separator);
        }

        if (canPrintConstant(getServiceInstance())) {
            sb.append("\"" + String.valueOf(getServiceInstance()) + "\"");
        } else {
            sb.append(typeLiteralName(getGuiceKey().getTypeLiteral()));
        }

        if (getProviderSource().isPresent()) {
            sb.append(separator).append("[Provider:");
            sb.append(typeLiteralName(getProviderSource().get().getGuiceKey().getTypeLiteral()));
            sb.append("]");
        }
        return sb.toString();
    }

    @VisibleForTesting
    String annotationTypeName(final Class<? extends Annotation> annotationType,
                              final Optional<Annotation> annotation) {
        final StringBuilder sb = new StringBuilder().append('@').append(annotationType.getSimpleName());
        annotation
            .filter(ann -> Arrays.asList(ann.getClass().getDeclaredMethods()).stream().anyMatch(method -> "value".equals(method.getName())))
            .map(ann -> {
                try {
                    final Method valueMethod = ann.getClass().getDeclaredMethod("value");
                    valueMethod.setAccessible(true);
                    return String.valueOf(valueMethod.invoke(ann));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).ifPresent(annValue -> sb.append('(').append(annValue).append(')'));

        return sb.toString();
    }

    @VisibleForTesting
    boolean canPrintConstant(final T serviceInstance) {
        if (!isConstant(serviceInstance)) {
            return false;
        }

        final String instanceStr = String.valueOf(serviceInstance);
        return instanceStr.length() < 64 ||
            // Give configurations a little more room to print
            (TaggedConfig.class.isAssignableFrom(serviceInstance.getClass()) &&
                instanceStr.length() < 128);
    }

    @VisibleForTesting
    String genericTypeName(final TypeLiteral<?> typeLiteral) {
        if (typeLiteral.getRawType().isArray()) {
            return simpleTypeName((Class) typeLiteral.getRawType());
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(simpleTypeName(typeLiteral.getRawType())).append('<');

        final ParameterizedType parameterizedType = (ParameterizedType) typeLiteral.getType();
        final List<String> parameterizedTypeNames = Arrays.asList(parameterizedType.getActualTypeArguments()).stream()
            .map(typeArg -> {
                if (typeArg instanceof WildcardType) {
                    return "?";
                }
                return typeLiteralName(TypeLiteral.get(typeArg));
            })
            .collect(Collectors.toList());

        sb.append(Joiner.on(", ").join(parameterizedTypeNames)).append('>');
        return sb.toString();
    }

    @VisibleForTesting
    boolean isConstant(final T serviceInstance) {
        return String.class.isAssignableFrom(serviceInstance.getClass())
            || Number.class.isAssignableFrom(serviceInstance.getClass())
            || Boolean.class.isAssignableFrom(serviceInstance.getClass())
            || Path.class.isAssignableFrom(serviceInstance.getClass())
            || TaggedConfig.class.isAssignableFrom(serviceInstance.getClass());
    }

    @VisibleForTesting
    String simpleTypeName(final Class<?> simpleType) {
        // Strip off the package names for standard Java classes, standard AppBuilder classes,
        // and standard Guice classes
        if (simpleType.getCanonicalName().startsWith("java.lang")
            || simpleType.getCanonicalName().startsWith("java.util")
            || simpleType.getCanonicalName().startsWith("net.spals.appbuilder")
            || simpleType.getCanonicalName().startsWith("com.google.inject")) {
            final List<String> nameParts = Splitter.on('.').splitToList(simpleType.getCanonicalName());
            final List<String> typeNameParts = nameParts.stream()
                .filter(namePart -> CharMatcher.javaUpperCase().matchesAnyOf(namePart))
                .collect(Collectors.toList());
            return Joiner.on('.').join(typeNameParts);
        }

        return simpleType.getCanonicalName();
    }

    @VisibleForTesting
    String typeLiteralName(final TypeLiteral<?> typeLiteral) {
        final StringBuilder sb = new StringBuilder();

        final java.lang.reflect.Type type = typeLiteral.getType();
        if (type instanceof Class) {
            sb.append(simpleTypeName((Class) type));
        } else {
            sb.append(genericTypeName(typeLiteral));
        }

        return sb.toString();
    }

}
