package org.opencds.cqf.fhir.cr.hapi.cdshooks;

public interface ICdsCrServiceFactory {
    ICdsCrService create(String serviceId);
}
