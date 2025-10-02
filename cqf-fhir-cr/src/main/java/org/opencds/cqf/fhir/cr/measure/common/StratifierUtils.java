package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// LUKETODO:  javadoc
public class StratifierUtils {

    // LUKETODO:  consider stratifier components?
    public static boolean isCriteriaBasedStratifierFromMeasureDefBuilder(
            GroupDef groupDef, StratifierDef stratifierDef) {
        var resultClasses = extractClassesFromSingleOrListResult(stratifierDef.getResultType());
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        // types of stratifiers
        // 1. path-based stratifier (FHIR path expression) >>> use the resource type of the population basis
        // 2. value-based stratifier >>> based on the values returned from that expression  ex age of the patient at
        // the
        // end of the measurement period   break down into stratums per value
        // 3. criteria stratifier NOT implement >> mix of the previous 2

        // LUKETODO: think about how to refine this:
        // LUKETODO: comment about component stratifiers

        return resultClasses.stream()
                .map(Class::getSimpleName)
                .anyMatch(simpleName -> simpleName.equals(groupPopulationBasisCode));
    }

    public static boolean isCriteriaBasedStratifierFromMeasureEvaluation(
            GroupDef groupDef, @Nullable Object expressionValue) {
        var resultClasses = extractClassesFromSingleOrListResult(expressionValue);
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        // types of stratifiers
        // 1. path-based stratifier (FHIR path expression) >>> use the resource type of the population basis
        // 2. value-based stratifier >>> based on the values returned from that expression  ex age of the patient at
        // the
        // end of the measurement period   break down into stratums per value
        // 3. criteria stratifier NOT implement >> mix of the previous 2

        // LUKETODO: think about how to refine this:
        // LUKETODO: comment about component stratifiers

        return resultClasses.stream()
                .map(Class::getSimpleName)
                .anyMatch(simpleName -> simpleName.equals(groupPopulationBasisCode));
    }

    private static List<Class<?>> extractClassesFromSingleOrListResult(Object result) {
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
