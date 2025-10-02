package org.opencds.cqf.fhir.cr.ecr;

import org.hl7.fhir.exceptions.FHIRException;

public class FhirResourceExistsException extends FHIRException {
    // Constructor without parameters
    public FhirResourceExistsException(String resourceType, String url, String version) {
        super("The specified entity: " + resourceType + " with " + url + " and version: " + version + " already exists.");
    }
}