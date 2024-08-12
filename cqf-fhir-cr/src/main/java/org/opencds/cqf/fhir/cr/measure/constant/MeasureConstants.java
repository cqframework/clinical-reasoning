package org.opencds.cqf.fhir.cr.measure.constant;

/*
constants used on Measure resources
 */
public class MeasureConstants {
    private MeasureConstants() {}

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
}
