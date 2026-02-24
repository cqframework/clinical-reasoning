package org.opencds.cqf.fhir.cr.measure.constant;

import java.sql.Date;

/*
constants used on MeasureReport resources
 */
public class MeasureReportConstants {
    private MeasureReportConstants() {}

    public static final String IMPROVEMENT_NOTATION_SYSTEM_INCREASE = "increase";
    public static final String IMPROVEMENT_NOTATION_SYSTEM_INCREASE_DISPLAY = "Increase";
    public static final String IMPROVEMENT_NOTATION_SYSTEM_DECREASE = "decrease";
    public static final String IMPROVEMENT_NOTATION_SYSTEM_DECREASE_DISPLAY = "Decrease";
    public static final String MEASUREREPORT_IMPROVEMENT_NOTATION_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/measure-improvement-notation";

    public static final String MEASUREREPORT_IMPROVEMENT_NOTATION_EXTENSION =
            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-improvementNotation";
    public static final String MEASUREREPORT_MEASURE_POPULATION_SYSTEM =
            "http://terminology.hl7.org/CodeSystem/measure-population";
    public static final String MEASUREREPORT_MEASURE_SUPPLEMENTALDATA_EXTENSION =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-supplementalData";
    public static final String MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_URL =
            "http://hl7.org/fhir/us/davinci-deqm/SearchParameter/measurereport-supplemental-data";
    public static final String MEASUREREPORT_PRODUCT_LINE_EXT_URL =
            "http://hl7.org/fhir/us/cqframework/cqfmeasures/StructureDefinition/cqfm-productLine";
    public static final String MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_VERSION = "0.1.0";
    public static final Date MEASUREREPORT_SUPPLEMENTALDATA_SEARCHPARAMETER_DEFINITION_DATE =
            Date.valueOf("2022-07-20"); // issue without local date?
    public static final String SDE_SYSTEM_URL = "http://terminology.hl7.org/CodeSystem/measure-data-usage";
    public static final String SDE_USAGE_CODE = "supplemental-data";
    public static final String RISK_ADJUSTMENT_USAGE_CODE = "risk-adjustment-factor";
    public static final String SDE_REFERENCE_EXT_URL =
            "http://hl7.org/fhir/5.0/StructureDefinition/extension-MeasureReport.supplementalDataElement.reference";
    public static final String SDE_DAVINCI_DEQM_EXT_URL =
            "http://hl7.org/fhir/us/davinci-deqm/StructureDefinition/extension-criteriaReference";
    public static final String COUNTRY_CODING_SYSTEM_CODE = "urn:iso:std:iso:3166";
    public static final String US_COUNTRY_CODE = "US";
    public static final String US_COUNTRY_DISPLAY = "United States of America";

    public static final String RESOURCE_TYPE_PRACTITIONER = "Practitioner";
    public static final String RESOURCE_TYPE_PRACTITIONER_ROLE = "PractitionerRole";
    public static final String RESOURCE_TYPE_ORGANIZATION = "Organization";
    public static final String RESOURCE_TYPE_LOCATION = "Location";
    public static final String RESOURCE_TYPE_PATIENT = "Patient";
    public static final String EXT_SUPPORTING_EVIDENCE_URL =
            "http://hl7.org/fhir/StructureDefinition/cqf-supportingEvidence";
}
