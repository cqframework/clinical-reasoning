package org.opencds.cqf.cql.evaluator.fhir;

public class Constants {

    private Constants() {
    }

    // public static final String CQF_QUESTIONNAIRE = "http://hl7.org/fhir/StructureDefinition/cqf-questionnaire";
    public static final String CQF_EXPRESSION = "http://hl7.org/fhir/StructureDefinition/cqf-expression";
    // This is only for dstu3 since the Expression type does not exist in that version
    public static final String CQF_EXPRESSION_LANGUAGE = "http://hl7.org/fhir/StructureDefinition/cqf-expression-language";
    public static final String CQF_LIBRARY =  "http://hl7.org/fhir/StructureDefinition/cqf-library";

    public static final String SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-itemExtractionContext";
    public static final String SDC_QUESTIONNAIRE_IS_SUBJECT = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-isSubject";
    public static final String SDC_QUESTIONNAIRE_RESPONSE_IS_SUBJECT = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaireresponse-isSubject";
    public static final String SDC_QUESTIONNAIRE_INITIAL_EXPRESSION = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression";
    public static final String SDC_QUESTIONNAIRE_LOOKUP_QUESTIONNAIRE = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-lookupQuestionnaire";
    public static final String SDC_QUESTIONNAIRE_PREPOPULATE = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-prepopulate";
}
