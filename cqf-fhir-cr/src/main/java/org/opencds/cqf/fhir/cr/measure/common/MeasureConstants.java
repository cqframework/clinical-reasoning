package org.opencds.cqf.fhir.cr.measure.common;

public class MeasureConstants {

    private MeasureConstants() {}

    public static final String URL_CODESYSTEM_MEASURE_POPULATION =
            "http://teminology.hl7.org/CodeSystem/measure-population";
    public static final String EXT_DAVINCI_POPULATION_REFERENCE =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-populationReference";
    public static final String EXT_SDE_URL =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData";
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
    public static final String POPULATION_BASIS_URL =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-populationBasis";
}
