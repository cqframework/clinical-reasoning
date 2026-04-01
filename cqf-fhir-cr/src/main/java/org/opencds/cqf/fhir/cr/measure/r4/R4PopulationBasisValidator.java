package org.opencds.cqf.fhir.cr.measure.r4;

import ca.uhn.fhir.context.FhirContext;
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
import org.opencds.cqf.cql.engine.runtime.Code;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cr.measure.common.GroupDef;
import org.opencds.cqf.fhir.cr.measure.common.MeasureDef;
import org.opencds.cqf.fhir.cr.measure.common.PopulationBasisValidator;

/**
 * Validates group populations and stratifiers against population basis-es for R4.
 * Provides R4-specific FHIR type mappings; all validation logic lives in
 * {@link PopulationBasisValidator} default methods.
 */
public class R4PopulationBasisValidator implements PopulationBasisValidator {

    private static final Set<Class<?>> ALLOWED_STRATIFIER_VALUE_TYPES = new HashSet<>(Arrays.asList(
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

    private static final String FHIR_MODEL_PACKAGE = "org.hl7.fhir.r4.model.";

    private static final List<String> RESOURCE_TYPE_NAMES =
            Arrays.stream(ResourceType.values()).map(ResourceType::name).toList();

    @Override
    public Set<Class<?>> allowedStratifierValueTypes() {
        return ALLOWED_STRATIFIER_VALUE_TYPES;
    }
    
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

        var cqlExpressionResult = evaluationResult.get(populationExpression);
        if (cqlExpressionResult == null || cqlExpressionResult.getValue() == null) {
            return;
        }
        var cqfFhirParameterConverter = Engines.getCqlFhirParametersConverter(FhirContext.forR4Cached());
        Object expressionResult;
        if (cqlExpressionResult.getValue() instanceof List<?> listValue) {
            expressionResult = listValue.stream()
                    .map(cqfFhirParameterConverter::convertToFhirIfNeeded)
                    .toList();
        } else {
            expressionResult = cqfFhirParameterConverter.convertToFhirIfNeeded(cqlExpressionResult.getValue());
        }

        var resultClasses = StratifierUtils.extractClassesFromSingleOrListResult(expressionResult);
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

    @Override
    public Optional<Class<?>> extractFhirResourceType(String groupPopulationBasisCode) {
        return resolveResourceType(groupPopulationBasisCode, RESOURCE_TYPE_NAMES, FHIR_MODEL_PACKAGE);
    }
}
