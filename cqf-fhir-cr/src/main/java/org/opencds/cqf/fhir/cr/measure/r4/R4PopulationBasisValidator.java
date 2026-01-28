package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Range;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.opencds.cqf.cql.engine.execution.EvaluationResult;
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;
import org.opencds.cqf.fhir.cr.measure.common.PopulationDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierDef;
import org.opencds.cqf.fhir.cr.measure.common.StratifierUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates group populations and stratifiers against population basis-es for R4 only.
 */
public class R4PopulationBasisValidator implements PopulationBasisValidator {

    private static final String BOOLEAN_BASIS = "boolean";

    /**
     * For any given stratifier expression, we don't know if it was evaluated in the patient context or it's a valid
     * expression, so we'll apply this heuristic for now
     */
    private static final Set<Class<?>> ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES = new HashSet<>(Arrays.asList(
            CodeableConcept.class,
            Quantity.class,
            Range.class,
            Reference.class,
            Coding.class,
            Enumeration.class,
            Boolean.class,
            // added Integer and String for examples like age or gender
            Integer.class,
            String.class,
            // CQL type returned by some stratifier expression that don't map neatly to FHIR types
            Code.class));

    private static final Logger log = LoggerFactory.getLogger(R4PopulationBasisValidator.class);

    @Override
    public void validateGroupPopulations(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult) {
        groupDef.populations()
                .forEach(population ->
                        validateGroupPopulationBasisType(measureDef.url(), groupDef, population, evaluationResult));
    }

    @Override
    public void validateStratifiers(MeasureDef measureDef, GroupDef groupDef, EvaluationResult evaluationResult) {
        groupDef.stratifiers()
                .forEach(stratifier -> validateStratifierPopulationBasisType(
                        measureDef.url(), groupDef, stratifier, evaluationResult));
    }

    private void validateGroupPopulationBasisType(
            String url, GroupDef groupDef, PopulationDef populationDef, EvaluationResult evaluationResult) {

        // PROPORTION
        var scoring = groupDef.measureScoring();
        // Numerator
        var populationExpression = populationDef.expression();
        if (populationExpression == null || populationExpression.isBlank()) {
            return;
        }

        var expressionResult = evaluationResult.get(populationExpression);

        if (expressionResult == null || expressionResult.getValue() == null) {
            return;
        }

        var resultClasses = StratifierUtils.extractClassesFromSingleOrListResult(expressionResult.getValue());
        // Encounter
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();
        var optResourceClass = extractResourceType(groupPopulationBasisCode);

        if (optResourceClass.isPresent()) {

            var resultMatchingClasses = resultClasses.stream()
                    .filter(it -> optResourceClass.get().isAssignableFrom(it))
                    .toList();

            if (resultMatchingClasses.size() != resultClasses.size()) {
                throw new InvalidRequestException(
                        "group expression criteria results for expression: [%s] and scoring: [%s] must fall within accepted types for population basis: [%s] for Measure: [%s] due to mismatch between total result classes: %s and matching result classes: %s"
                                .formatted(
                                        populationExpression,
                                        scoring,
                                        groupPopulationBasisCode,
                                        url,
                                        prettyClassNames(resultClasses),
                                        prettyClassNames(resultMatchingClasses)));
            }
        }
    }

    private void validateStratifierPopulationBasisType(
            String url, GroupDef groupDef, StratifierDef stratifierDef, EvaluationResult evaluationResult) {

        if (stratifierDef.isCriteriaStratifier()) {
            validateExpressionResultType(groupDef, stratifierDef, stratifierDef.expression(), evaluationResult, url);
        } else {
            for (var component : stratifierDef.components()) {
                validateExpressionResultType(groupDef, stratifierDef, component.expression(), evaluationResult, url);
            }
        }
    }

    private void validateExpressionResultType(
            GroupDef groupDef,
            StratifierDef stratifierDef,
            String expression,
            EvaluationResult evaluationResult,
            String url) {

        var expressionResult = evaluationResult.get(expression);

        if (expressionResult == null || expressionResult.getValue() == null) {
            return;
        }

        var resultClasses = StratifierUtils.extractClassesFromSingleOrListResult(expressionResult.getValue());
        var groupPopulationBasisCode = groupDef.getPopulationBasis().code();

        if (stratifierDef.isCriteriaStratifier()) {
            if (resultClasses.isEmpty()) {
                log.warn("Criteria-based stratifier results are empty for measure: {}", url);
                return;
            }

            if (resultClasses.stream()
                    .noneMatch(resultClass -> doesBasisMatchResource(resultClass, groupPopulationBasisCode))) {

                throw new InvalidRequestException(
                        "criteria-based stratifier is invalid for expression: [%s] due to mismatch between population basis: [%s] and result types: %s for measure URL: %s"
                                .formatted(expression, groupPopulationBasisCode, prettyClassNames(resultClasses), url));
            }

            // skip validation below since for criteria-based stratifier, the boolean basis test is irrelevant
            return;
        }

        var resultMatchingClasses = resultClasses.stream()
                .filter(resultClass ->
                        ALLOWED_STRATIFIER_BOOLEAN_BASIS_TYPES.contains(resultClass) || Boolean.class == resultClass)
                .toList();

        if (resultMatchingClasses.size() != resultClasses.size()) {
            throw new InvalidRequestException(
                    "stratifier expression criteria results for expression: [%s] must fall within accepted types for population-basis: [%s] for Measure: [%s] due to mismatch between total eval result classes: %s and matching result classes: %s"
                            .formatted(
                                    expression,
                                    groupPopulationBasisCode,
                                    url,
                                    prettyClassNames(resultClasses),
                                    prettyClassNames(resultMatchingClasses)));
        }
    }

    private boolean doesBasisMatchResource(Class<?> resultClass, String groupPopulationBasisCode) {
        // If we don't do this we'll fail with "boolean" vs. "Boolean"
        if (resultClass == Boolean.class && BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            return true;
        }

        return resultClass.getSimpleName().equals(groupPopulationBasisCode);
    }

    private Optional<Class<?>> extractResourceType(String groupPopulationBasisCode) {
        if (BOOLEAN_BASIS.equals(groupPopulationBasisCode)) {
            return Optional.of(Boolean.class);
        }

        final Optional<String> optResourceClassName = Arrays.stream(ResourceType.values())
                .map(ResourceType::name)
                .filter(theName -> {
                    if ("ListResource".equals(groupPopulationBasisCode)) {
                        return true;
                    }

                    return theName.equals(groupPopulationBasisCode);
                })
                .map(typeName -> "org.hl7.fhir.r4.model." + typeName)
                .findFirst();

        if (optResourceClassName.isPresent()) {
            try {
                return Optional.of(Class.forName(optResourceClassName.get()));
            } catch (ClassNotFoundException exception) {
                throw new InternalErrorException(exception);
            }
        }
        return Optional.empty();
    }

    private List<String> prettyClassNames(List<Class<?>> classes) {
        return classes.stream().map(Class::getSimpleName).toList();
    }
}
