package org.opencds.cqf.fhir.cr.measure.common;

import static org.opencds.cqf.fhir.cql.ClassInstanceHelper.getClassName;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.opencds.cqf.cql.engine.runtime.ClassInstance;
import org.opencds.cqf.fhir.cql.ClassInstanceHelper;

/**
 * Various FHIR version-agnostic utilities for working with Stratifiers.
 */
public class StratifierUtils {

    private StratifierUtils() {
        // Static utility class
    }

    public static List<String> extractClassesFromSingleOrListResult(CqlExpressionValue value) {
        if (value.isNull()) {
            return Collections.emptyList();
        }

        Object raw = value.raw();
        if (raw instanceof Class<?> clazz) {
            return List.of(clazz.getName());
        }

        if (!value.isIterable()) {
            if (raw instanceof ClassInstance classInstance) {
                return List.of(ClassInstanceHelper.getClassName(classInstance));
            }
            return Collections.emptyList();
        }

        return getStream(value.asIterable())
                .filter(Objects::nonNull)
                .map(o -> o instanceof ClassInstance classInstance
                        ? getClassName(classInstance)
                        : o.getClass().getName())
                .toList();
    }

    private static Stream<?> getStream(Iterable<?> iterable) {
        if (iterable instanceof List<?> list) {
            return list.stream();
        }

        // It's entirely possible CQL returns an Iterable that is not a List, so we need to handle that case
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
