package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Various FHIR version-agnostic utilities for working with Stratifiers.
 */
public class StratifierUtils {

    public static boolean isCriteriaBasedStratifier(GroupDef groupDef, StratifierDef stratifierDef) {
        return isCriteriaBasedStratifier(groupDef, stratifierDef.getResultType());
    }

    public static boolean isCriteriaBasedStratifier(GroupDef groupDef, @Nullable Object expressionValue) {
        var resultClasses = extractClassesFromSingleOrListResult(expressionValue);
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        return resultClasses.stream()
                .map(Class::getSimpleName)
                .anyMatch(simpleName -> simpleName.equals(groupPopulationBasisCode));
    }

    public static List<Class<?>> extractClassesFromSingleOrListResult(Object result) {
        if (result == null) {
            return Collections.emptyList();
        }

        if (result instanceof Class<?> clazz) {
            return List.of(clazz);
        }

        if (!(result instanceof Iterable<?> iterable)) {
            return List.of(result.getClass());
        }

        // Need to this to return List<Class<?>> and get rid of Sonar warnings.
        final Stream<Class<?>> classStream =
                getStream(iterable).filter(Objects::nonNull).map(Object::getClass);

        return classStream.toList();
    }

    private static Stream<?> getStream(Iterable<?> iterable) {
        if (iterable instanceof List<?> list) {
            return list.stream();
        }

        // It's entirely possible CQL returns an Iterable that is not a List, so we need to handle that case
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
