package org.opencds.cqf.cql.evaluator.builder;

import java.util.HashMap;
import java.util.Map;

public class Constants {

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