package net.spals.appbuilder.mapstore.core.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * @author tkral
 */
public interface MapRangeOperator {

    enum Standard implements MapRangeOperator {
        ALL,
        BETWEEN,
        EQUAL_TO,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL_TO,
        LESS_THAN,
        LESS_THAN_OR_EQUAL_TO,
        MAX,
        MIN,
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
}
