package org.opencds.cqf.fhir.cr.measure.constant;

/*
constants used on Care-Gap returned resources
 */
public class CareGapsConstants {

    private CareGapsConstants() {
        // intentionally empty
    }

    public static final String CARE_GAPS_REPORT_PROFILE =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/indv-measurereport-deqm";
    public static final String CARE_GAPS_BUNDLE_PROFILE =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-bundle-deqm";
    public static final String CARE_GAPS_COMPOSITION_PROFILE =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-composition-deqm";
    public static final String CARE_GAPS_DETECTED_ISSUE_PROFILE =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-detectedissue-deqm";
    public static final String CARE_GAPS_DETECTED_ISSUE_MR_GROUP_ID =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/gaps-detectedissue-mr-group-id-deqm";
    public static final String CARE_GAPS_GAP_STATUS_EXTENSION =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-gapStatus";
    public static final String CARE_GAPS_GAP_STATUS_SYSTEM =
            "http://hl7.org/fhir/us/davinci-deqm/CodeSystem/gaps-status";
    public static final String CARE_GAPS_REPORTER_KEY = "care_gaps_reporter";
    public static final String CARE_GAPS_SECTION_AUTHOR_KEY = "care_gaps_composition_section_author";
}
