package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

public interface ICrDiscoveryServiceFactory {
    ICrDiscoveryService create(String serviceId);
}
