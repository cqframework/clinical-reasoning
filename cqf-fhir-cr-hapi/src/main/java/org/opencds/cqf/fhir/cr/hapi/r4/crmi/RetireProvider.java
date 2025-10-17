package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.fhir.cr.hapi.r4.IRetireServiceFactory;

public class RetireProvider {

    private final IRetireServiceFactory r4RetireServiceFactory;

    public RetireProvider(IRetireServiceFactory r4RetireServiceFactory) {
        this.r4RetireServiceFactory = r4RetireServiceFactory;
    }

    /**
     * Retires an existing artifact if it has status Draft.
     *
     * @param id the {@link IdType IdType}, always an argument for instance level operations
     * @return A transaction {@link Bundle Bundle} result of the retired resources
     */
    @Operation(name = "$retire", idempotent = true, global = true, type = MetadataResource.class)
    @Description(shortDefinition = "$retire", value = "Retire an existing active artifact")
    public Bundle withdrawOperation(@IdParam IdType id, RequestDetails requestDetails) throws FHIRException {
        return r4RetireServiceFactory.create(requestDetails).retire(id);
    }
}
