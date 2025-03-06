package org.opencds.cqf.fhir.cr.hapi.cdshooks;

public class CdsCrConstants {
    private CdsCrConstants() {}

    public static final String CDS_CR_MODULE_ID = "CR";

    // CDS Hook field names
    public static final String CDS_PARAMETER_USER_ID = "userId";
    public static final String CDS_PARAMETER_PATIENT_ID = "patientId";
    public static final String CDS_PARAMETER_ENCOUNTER_ID = "encounterId";
    public static final String CDS_PARAMETER_MEDICATIONS = "medications";
    public static final String CDS_PARAMETER_PERFORMER = "performer";
    public static final String CDS_PARAMETER_TASK = "task";
    public static final String CDS_PARAMETER_ORDERS = "orders";
    public static final String CDS_PARAMETER_SELECTIONS = "selections";
    public static final String CDS_PARAMETER_DRAFT_ORDERS = "draftOrders";
    public static final String CDS_PARAMETER_APPOINTMENTS = "appointments";

    // $apply parameter names
    public static final String APPLY_PARAMETER_PLAN_DEFINITION = "planDefinition";
    public static final String APPLY_PARAMETER_CANONICAL = "canonical";
    public static final String APPLY_PARAMETER_SUBJECT = "subject";
    public static final String APPLY_PARAMETER_PRACTITIONER = "practitioner";
    public static final String APPLY_PARAMETER_ENCOUNTER = "encounter";
    public static final String APPLY_PARAMETER_PARAMETERS = "parameters";
    public static final String APPLY_PARAMETER_DATA = "data";
    public static final String APPLY_PARAMETER_DATA_ENDPOINT = "dataEndpoint";
}
