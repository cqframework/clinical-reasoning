package org.opencds.cqf.fhir.cr.measure.r4;

import static java.util.function.Predicate.not;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Range;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Type;
import org.opencds.cqf.cql.engine.execution.ExpressionResult;
import org.opencds.cqf.fhir.cr.measure.common.CriteriaResult;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;

// LUKETODO: javadoc
public class R4PopulationBasisValidator {

    private static final String BOOLEAN_BASIS = "boolean";

    // LUKETODO:
    enum PopulationBasis {
        NONE,
        BOOLEAN,
        ENCOUNTER,
        NON_ENCOUNTER_RESOURCE
    }

    // LUKETODO:  animal sniffer
    // LUKETODO:  we're failing because "Code" doesn't match "CodeableConcept" so investigate
    private static final Set<Class<? extends Type>> ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES =
            Set.of(CodeableConcept.class, Quantity.class, Range.class, Reference.class);

    // LUKETODO: stratifier:  for boolean basis validate that:  CodeableConcept|boolean|Quantity|Range|Reference

    // LUKETODO:  pass actual measure later?
    public void validateGroupPopulationBasisType(String url, GroupDef groupDef, ExpressionResult expressionResult) {
        printDetails(url, groupDef, expressionResult.value());

        var resultClasses = extractClassesFromSingleOrListResult(expressionResult.value());
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        var optResourceClass = extractResourceType(groupPopulationBasisCode);

        if (optResourceClass.isPresent()) {

            var resultMatchingClassCount = resultClasses.stream()
                    .filter(it -> optResourceClass.get().isAssignableFrom(it))
                    .count();

            if (resultMatchingClassCount != resultClasses.size()) {
                throw new InvalidRequestException(String.format(
                        "group expression criteria results must match the same type as population basis: [%s] for Measure: %s",
                        groupPopulationBasisCode, url));
            }
        }

        // LUKETODO:  what if it's empty?
    }

    public void validateStratifierPopulationBasisType(
            String url, GroupDef groupDef, ExpressionResult expressionResult) {
        printDetails(url, groupDef, expressionResult.value());

        var resultClasses = extractClassesFromSingleOrListResult(expressionResult.value());
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            var resultMatchingClassCount = resultClasses.stream()
                    .filter(resultClass -> ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES.contains(resultClass)
                            || Boolean.class == resultClass)
                    .count();

            if (resultMatchingClassCount != resultClasses.size()) {
                throw new InvalidRequestException(String.format(
                        "stratifier expression criteria results must match the same type as population basis: [%s] for Measure: %s",
                        groupPopulationBasisCode, url));
            }

            return;
        }

        var optResourceClass = extractResourceType(groupPopulationBasisCode);

        if (optResourceClass.isPresent()) {

            var resultMatchingClassCount = resultClasses.stream()
                    .filter(it -> optResourceClass.get().isAssignableFrom(it))
                    .count();

            if (resultMatchingClassCount != resultClasses.size()) {
                throw new InvalidRequestException(String.format(
                        "stratifier expression criteria results must match the same type as population basis: [%s] for Measure: %s",
                        groupPopulationBasisCode, url));
            }
        }

        // LUKETODO:  what if it's empty?
    }

    public void validateStratifierPopulationBasisType(
            Measure measure, Map<String, CriteriaResult> subjectValues, GroupDef groupDef) {
        var isBooleanBasisDirect = groupDef.isBooleanBasis();

        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        var isBooleanBasisindirect = BOOLEAN_BASIS.equals(groupPopulationBasisCode);

        var criteriaResultsClassesStream = subjectValues.values().stream()
                .map(CriteriaResult::rawValue)
                .map(Object::getClass)
                .toList();

        // LUKETODO:  why are these org.opencds.cqf.cql.engine.runtime.Code if these are
        //        System.out.printf("populationBasis: [%s], criteriaClasses: %s, criteriaResults: %s\n",
        // groupPopulationBasisCode,
        // subjectValues.values().stream().map(CriteriaResult::rawValue).map(Object::getClass).distinct().collect(Collectors.toList()), subjectValues.values().stream().map(CriteriaResult::rawValue).distinct().collect(Collectors.toList()));
        //        System.out.printf("validateStratifierBasisType(): populationBasis: [%s], criteriaClasses: %s\n",
        // groupPopulationBasisCode,
        // subjectValues.values().stream().map(CriteriaResult::rawValue).map(Object::getClass).distinct().collect(Collectors.toList()));

        if (!subjectValues.entrySet().isEmpty() && !isBooleanBasisindirect) {
            var list = subjectValues.values().stream()
                    .filter(x -> x.rawValue() instanceof Resource)
                    .collect(Collectors.toList());
            if (list.size() != subjectValues.values().size()) {
                throw new InvalidRequestException(
                        "stratifier expression criteria results must match the same type as population for Measure: "
                                + measure.getUrl());
            }
        }

        var criteriaResultsClassesStreamStream = criteriaResultsClassesStream.stream();

        var errorMessageTemplate =
                "stratifier expression criteria results must match the same type: %s as population: %s for Measure: "
                        + measure.getUrl();

        switch (groupPopulationBasisCode) {
            case BOOLEAN_BASIS:
                var booleanCount = criteriaResultsClassesStreamStream
                        .filter(Boolean.class::isAssignableFrom)
                        .count();

                if (booleanCount != subjectValues.values().size()) {
                    //                    // LUKETODO:  better error message
                    //                    throw new InvalidRequestException(
                    //                        "stratifier expression criteria results must match the same type as
                    // population for Measure: "
                    //                            + measure.getUrl());
                }
                break;
            case "Encounter":
                var encounterCount = criteriaResultsClassesStreamStream
                        .filter(Encounter.class::isAssignableFrom)
                        .count();

                if (encounterCount != subjectValues.values().size()) {
                    //                    // LUKETODO:  better error message
                    //                    throw new InvalidRequestException(
                    //                        "stratifier expression criteria results must match the same type as
                    // population for Measure: "
                    //                            + measure.getUrl());
                }
                break;
            default:
                // LUKETODO: TEST THIS!!!!!
                var nonEncounterResourceCount = criteriaResultsClassesStreamStream
                        .filter(Resource.class::isAssignableFrom)
                        .filter(not(Encounter.class::isAssignableFrom))
                        .count();

                if (nonEncounterResourceCount != subjectValues.values().size()) {
                    // LUKETODO:  better error message
                    //                    throw new InvalidRequestException(
                    //                        "stratifier expression criteria results must match the same type as
                    // population for Measure: "
                    //                            + measure.getUrl());
                }
        }
    }

    private Optional<? extends Class<?>> extractResourceType(String groupPopulationBasisCode) {
        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            return Optional.of(Boolean.class);
        }
        return Arrays.stream(ResourceType.values())
                .filter(resourceType -> {
                    if ("ListResource".equals(groupPopulationBasisCode)) {
                        return true;
                    }

                    return resourceType.name().equals(groupPopulationBasisCode);
                })
                .map(ResourceType::name)
                .map(typeName -> "org.hl7.fhir.r4.model." + typeName)
                .map(fullyQualified -> {
                    try {
                        return Class.forName(fullyQualified);
                    } catch (Exception exception) {
                        throw new IllegalArgumentException(exception);
                    }
                })
                .findFirst();
    }

    // LUKETODO:  javadoc
    protected void validateGroupBasisType(Map<String, CriteriaResult> subjectValues, boolean isBooleanBasis) {

        // LUKETODO:  validate boolean basis doesn't match CriteriaResults

        if (!subjectValues.entrySet().isEmpty() && !isBooleanBasis) {
            var list = subjectValues.values().stream()
                    .filter(x -> x.rawValue() instanceof Resource)
                    .collect(Collectors.toList());
            if (list.size() != subjectValues.values().size()) {
                throw new IllegalArgumentException(
                        "stratifier expression criteria results must match the same type as population.");
            }
        }
    }

    /**
     *
     * Resource result --> Patient Key, Resource result --> can intersect on patient for Boolean basis, can't for Resource
     * boolean result --> Patient Key, Boolean result --> can intersect on Patient
     * code result --> Patient Key, Code result --> can intersect on Patient
     */
    protected void validateStratifierBasisType(Map<String, CriteriaResult> subjectValues, boolean isBooleanBasis) {

        // LUKETODO:  validate boolean basis doesn't match CriteriaResults

        if (!subjectValues.entrySet().isEmpty() && !isBooleanBasis) {
            var list = subjectValues.values().stream()
                    .filter(x -> x.rawValue() instanceof Resource)
                    .collect(Collectors.toList());
            if (list.size() != subjectValues.values().size()) {
                throw new IllegalArgumentException(
                        "stratifier expression criteria results must match the same type as population.");
            }
        }
    }

    private PopulationBasis extractPopulationBasis(GroupDef groupDef) {
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            return PopulationBasis.BOOLEAN;
        }

        switch (groupPopulationBasisCode) {
            case BOOLEAN_BASIS:
                return PopulationBasis.BOOLEAN;
            case "Encounter":
                return PopulationBasis.ENCOUNTER;
            default:
                if (Arrays.stream(ResourceType.values()).anyMatch(type -> {
                    if (ResourceType.List == type) {
                        return "ListResource".equals(groupPopulationBasisCode);
                    }
                    return type.name().equals(groupPopulationBasisCode);
                })) {
                    return PopulationBasis.NON_ENCOUNTER_RESOURCE;
                }
                // LUKETODO:
                return PopulationBasis.NONE;
        }
    }

    private List<Class<?>> extractClassesFromSingleOrListResult(Object result) {
        if (result == null) {
            return Collections.emptyList();
        }

        if (!(result instanceof List<?>)) {
            return Collections.singletonList(result.getClass());
        }

        var list = (List<?>) result;

        return list.stream().filter(Objects::nonNull).map(Object::getClass).collect(Collectors.toList());
    }

    // LUKETODO:  get rid of this at the last minute:
    private void printDetails(String url, GroupDef groupDef, Object result) {
        final String resultClass;
        if (result != null) {
            if (result instanceof List<?> list) {
                if (!list.isEmpty()) {
                    resultClass = "List: " + list.get(0).getClass().getSimpleName();
                } else {
                    resultClass = "List: " + null;
                }
            } else {
                resultClass = "Single: " + result.getClass().getSimpleName();
            }
        } else {
            resultClass = "Single: " + null;
        }

        System.out.printf(
                "%s: populationBasis: [%s], result class: %s\n",
                url, groupDef.getPopulationBasis().code(), resultClass);
    }
}
