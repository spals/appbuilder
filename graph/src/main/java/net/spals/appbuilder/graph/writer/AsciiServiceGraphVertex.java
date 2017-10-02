package net.spals.appbuilder.graph.writer;

//import com.google.common.annotations.VisibleForTesting;
//import com.google.common.base.CharMatcher;
//import com.google.common.base.Joiner;
//import com.google.common.base.Splitter;
//import com.google.inject.TypeLiteral;
//import net.spals.appbuilder.graph.model.ServiceGraphVertex;
//
//import java.lang.annotation.Annotation;
//import java.lang.reflect.Method;
//import java.lang.reflect.ParameterizedType;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;

/**
 * @author tkral
 */
class AsciiServiceGraphVertex {

//    private final ServiceGraphVertex vertexDelegate;
//
//    AsciiServiceGraphVertex(final ServiceGraphVertex vertexDelegate) {
//        this.vertexDelegate = vertexDelegate;
//    }
//
//    @Override
//    public String toString() {
//        final Class<? extends Annotation> serviceAnnotationType = vertexDelegate.getGuiceKey().getAnnotationType();
//
//        final StringBuilder sb = new StringBuilder();
//        if (serviceAnnotationType != null) {
//            sb.append(annotationTypeName(serviceAnnotationType, Optional.ofNullable(vertexDelegate.getGuiceKey().getAnnotation()))).append(' ');
//        }
//
//        sb.append(typeLiteralName(vertexDelegate.getGuiceKey().getTypeLiteral()));
//        return sb.toString();
//    }
//
//    @VisibleForTesting
//    String annotationTypeName(final Class<? extends Annotation> annotationType,
//                              final Optional<Annotation> annotation) {
//        final StringBuilder sb = new StringBuilder().append('@').append(annotationType.getSimpleName());
//        annotation
//                .filter(ann -> Arrays.asList(ann.getClass().getDeclaredMethods()).stream().anyMatch(method -> "value".equals(method.getName())))
//                .map(ann -> {
//                    try {
//                        final Method valueMethod = ann.getClass().getDeclaredMethod("value");
//                        valueMethod.setAccessible(true);
//                        return String.valueOf(valueMethod.invoke(ann));
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                }).ifPresent(annValue -> sb.append('(').append(annValue).append(')'));
//
//        return sb.toString();
//    }
//
//    @VisibleForTesting
//    String genericTypeName(final TypeLiteral<?> genericTypeLiteral) {
//        if (genericTypeLiteral.getRawType().isArray()) {
//            return simpleTypeName((Class) genericTypeLiteral.getRawType());
//        }
//
//        final StringBuilder sb = new StringBuilder();
//        sb.append(simpleTypeName(genericTypeLiteral.getRawType())).append('<');
//
//        final ParameterizedType parameterizedType = (ParameterizedType) genericTypeLiteral.getType();
//        final List<String> parameterizedTypeNames = Arrays.asList(parameterizedType.getActualTypeArguments()).stream()
//                .map(typeArg -> typeLiteralName(TypeLiteral.get(typeArg)))
//                .collect(Collectors.toList());
//
//        sb.append(Joiner.on(", ").join(parameterizedTypeNames)).append('>');
//        return sb.toString();
//    }
//
//    @VisibleForTesting
//    String simpleTypeName(final Class<?> simpleType) {
//        // Strip off the package names for standard Java classes, standard AppBuilder classes,
//        // and standard Guice classes
//        if (simpleType.getCanonicalName().startsWith("java.lang") || simpleType.getCanonicalName().startsWith("java.util")
//                || simpleType.getCanonicalName().startsWith("net.spals.appbuilder")
//                || simpleType.getCanonicalName().startsWith("com.google.inject")) {
//            final List<String> nameParts = Splitter.on('.').splitToList(simpleType.getCanonicalName());
//            final List<String> typeNameParts = nameParts.stream()
//                    .filter(namePart -> CharMatcher.javaUpperCase().matchesAnyOf(namePart))
//                    .collect(Collectors.toList());
//            return Joiner.on('.').join(typeNameParts);
//        }
//
//        return simpleType.getCanonicalName();
//    }
//
//    @VisibleForTesting
//    String typeLiteralName(final TypeLiteral<?> typeLiteral) {
//        final StringBuilder sb = new StringBuilder();
//
//        final java.lang.reflect.Type type = typeLiteral.getType();
//        if (type instanceof Class) {
//            sb.append(simpleTypeName((Class) type));
//        } else {
//            sb.append(genericTypeName(typeLiteral));
//        }
//
//        return sb.toString();
//    }
}
