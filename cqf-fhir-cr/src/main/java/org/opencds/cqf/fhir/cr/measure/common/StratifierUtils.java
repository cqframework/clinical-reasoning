package org.opencds.cqf.fhir.cr.measure.common;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.opencds.cqf.fhir.cr.measure.MeasureStratifierType;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants;

/**
 * Various FHIR version-agnostic utilities for working with Stratifiers.
 */
public class StratifierUtils {

    private StratifierUtils() {
        // Static utility class
    }

    public static MeasureStratifierType getStratifierType(
            org.hl7.fhir.dstu3.model.Measure.MeasureGroupStratifierComponent measureGroupStratifierComponent) {
        if (measureGroupStratifierComponent == null) {
            return MeasureStratifierType.VALUE;
        }

        final List<org.hl7.fhir.dstu3.model.Extension> stratifierExtensions =
                measureGroupStratifierComponent.getExtension();

        if (CollectionUtils.isEmpty(stratifierExtensions)) {
            return MeasureStratifierType.VALUE;
        }

        if (stratifierExtensions.stream()
                .filter(ext -> MeasureConstants.EXT_STRATIFIER_TYPE.equals(ext.getUrl()))
                .map(org.hl7.fhir.dstu3.model.Extension::getValue)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .anyMatch("value"::equals)) {
            return MeasureStratifierType.CRITERIA;
        }

        return MeasureStratifierType.VALUE;
    }

    public static MeasureStratifierType getStratifierType(
            MeasureGroupStratifierComponent measureGroupStratifierComponent) {
        if (measureGroupStratifierComponent == null) {
            return MeasureStratifierType.VALUE;
        }

        final List<Extension> stratifierExtensions = measureGroupStratifierComponent.getExtension();

        if (CollectionUtils.isEmpty(stratifierExtensions)) {
            return MeasureStratifierType.VALUE;
        }

        if (stratifierExtensions.stream()
                .filter(ext -> MeasureConstants.EXT_STRATIFIER_TYPE.equals(ext.getUrl()))
                .map(Extension::getValue)
                .filter(Objects::nonNull)
                .filter(CodeType.class::isInstance)
                .map(CodeType.class::cast)
                .map(PrimitiveType::asStringValue)
                .anyMatch(MeasureStratifierType.CRITERIA.getTextValue()::equals)) {
            return MeasureStratifierType.CRITERIA;
        }

        return MeasureStratifierType.VALUE;
    }

    // LUKETODO:  turn this into validation only
    public static boolean validateStratifier(GroupDef groupDef, @Nullable Object expressionValue) {
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
