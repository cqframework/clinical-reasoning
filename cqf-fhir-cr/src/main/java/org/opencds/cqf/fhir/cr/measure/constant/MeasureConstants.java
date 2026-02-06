package org.opencds.cqf.fhir.cr.measure.constant;

/*
constants used on Measure resources
 */
public class MeasureConstants {
    private MeasureConstants() {}

    public static final String CQFM_SCORING_SYSTEM_URL = "http://terminology.hl7.org/ValueSet/measure-scoring";
    public static final String CQFM_SCORING_EXT_URL =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-scoring";
    // this is used on Measure resources to indicate to $care-gaps and $care-list that the measure is compatible for the
    // operation.
    public static final String CQFM_CARE_GAP_COMPATIBLE_EXT_URL =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-care-gap-compatible";
    // this indicates to $evaluate-measure to execute cql expression with referenced cql expression value
    // when indicated it will only evaluate for individual reportType or subject EvalType
    public static final String CQFM_CARE_GAP_DATE_OF_COMPLIANCE_EXT_URL =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-care-gap-date-of-compliance-expression";

    public static final String URL_CODESYSTEM_MEASURE_POPULATION =
            "http://terminology.hl7.org/CodeSystem/measure-population";
    // http://hl7.org/fhir/us/davinci-deqm/2023Jan/StructureDefinition-extension-populationReference.html
    public static final String EXT_CQFM_AGGREGATE_METHOD_URL =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-aggregateMethod";
    public static final String EXT_CQFM_CRITERIA_REFERENCE =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference";
    public static final String EXT_DAVINCI_POPULATION_REFERENCE =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-populationReference";
    // http://build.fhir.org/ig/HL7/davinci-deqm/StructureDefinition-extension-supplementalData.html
    public static final String EXT_SDE_URL =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData";
    public static final String EXT_SUPPORTING_EVIDENCE_DEFINITION_URL =
            "http://hl7.org/fhir/StructureDefinition/cqf-supportingEvidenceDefinition";
    // http://hl7.org/fhir/us/davinci-deqm/2023Jan/StructureDefinition-extension-criteriaReference.html
    public static final String EXT_CRITERIA_REFERENCE_URL =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-criteriaReference";
    public static final String EXT_SDE_DISAGGREGATE_URL =
            "http://hl7.org/fhir/5.0/StructureDefinition/extension-Measure.supplementalDataElement.disaggregate";
    public static final String EXT_POPULATION_DESCRIPTION_URL =
            "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.population.description";
    public static final String EXT_SDE_REFERENCE_URL =
            "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.supplementalDataElement.reference";
    public static final String EXT_OPERATION_OUTCOME_REFERENCE_URL =
            "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.operationOutcome.reference";
    public static final String MEASUREMENT_PERIOD_PARAMETER_NAME = "Measurement Period";
    public static final String FHIR_MODEL_URI = "http://hl7.org/fhir";
    public static final String FHIR_ALL_TYPES_SYSTEM_URL = "http://hl7.org/fhir/fhir-types";
    public static final String POPULATION_BASIS_URL =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-populationBasis";

    // Capture the numerator or denominator aggregation result in the population
    public static final String EXT_AGGREGATION_METHOD_RESULT =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/extension-aggregationMethodResult";
    public static final String EXT_CQF_EXPRESSION_CODE = "http://hl7.org/fhir/StructureDefinition/cqf-expressionCode";
}
