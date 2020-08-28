package org.opencds.cqf.cql.evaluator.builder.api;

import java.util.HashMap;
import java.util.Map;

import ca.uhn.fhir.model.api.Tag;

public class Constants {

    public static final String HL7_FHIR_REST = "hl7-fhir-rest";
    public static final String HL7_FHIR_FILES = "hl7-fhir-files";
    public static final String HL7_CQL_FILES = "hl7-cql-files";

    public static final Tag HL7_FHIR_REST_CODE = new Tag(null, HL7_FHIR_REST);
	public static final Tag HL7_FHIR_FILES_CODE = new Tag(null, HL7_FHIR_FILES);
	public static final Tag HL7_CQL_FILES_CODE = new Tag(null,HL7_CQL_FILES);

    public static final String FHIR_MODEL_URI = "http://hl7.org/fhir";
    public static final String QDM_MODEL_URI = "urn:healthit-gov:qdm:v5_4";

    public static final Map<String, String> ALIAS_MAP = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("FHIR", FHIR_MODEL_URI);
            put("QUICK", FHIR_MODEL_URI);
            put("QDM", QDM_MODEL_URI);
        }
    };  
}