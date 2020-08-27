package org.opencds.cqf.cql.evaluator.builder.api.model;

public enum ConnectionType
{
	HL7_FHIR_REST("hl7-fhir-rest"),
	HL7_FHIR_FILES("hl7-fhir-files"),
	HL7_CQL_FILES("hl7-cql-files");

	private String type;
	
    ConnectionType(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}
}