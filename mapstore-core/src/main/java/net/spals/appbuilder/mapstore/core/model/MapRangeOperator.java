package net.spals.appbuilder.mapstore.core.model;

import net.spals.appbuilder.mapstore.core.MapStorePlugin;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * A range key operator which represents
 * how range keys should be filtered.
 *
 * @author tkral
 */
public interface MapRangeOperator {

    /**
     * Standard range operators. All {@link MapStorePlugin}s
     * are expected to implement these.
     */
    enum Standard implements MapRangeOperator {
        ALL,
        BETWEEN,
        EQUAL_TO,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL_TO,
        LESS_THAN,
        LESS_THAN_OR_EQUAL_TO,
        NONE,
        ;

        private static final Map<String, Standard> nameMap;

        static {
            nameMap = EnumSet.allOf(Standard.class).stream()
                    .collect(Collectors.toMap(op -> op.name(), identity()));
        }

        public static Optional<Standard> fromName(final String opName) {
            return Optional.ofNullable(nameMap.get(opName));
        }
    }

    /**
     * Extended range operators. These are operations that can
     * be supported by some, but not all, {@link MapStorePlugin}s.
     */
    enum Extended implements MapRangeOperator {
        IN,
        LIKE,
        STARTS_WITH,
        ;

        private static final Map<String, Extended> nameMap;

        static {
            nameMap = EnumSet.allOf(Extended.class).stream()
                    .collect(Collectors.toMap(op -> op.name(), identity()));
        }

        public static Optional<Extended> fromName(final String opName) {
            return Optional.ofNullable(nameMap.get(opName));
        }
    }

    /**
     * Operators which are syntactic sugar for
     * common multi-step operations.
     *
     * NOTE: These are implemented in the
     * DelegatingMapStore and need not be
     * implemented within individual {@link MapStorePlugin}s.
     */
    enum SyntacticSugar implements MapRangeOperator {
        MAX,
        MIN,
        ;

        private static final Map<String, SyntacticSugar> nameMap;

        static {
            nameMap = EnumSet.allOf(SyntacticSugar.class).stream()
                    .collect(Collectors.toMap(op -> op.name(), identity()));
        }

        public static Optional<SyntacticSugar> fromName(final String opName) {
            return Optional.ofNullable(nameMap.get(opName));
        }
    }
}
