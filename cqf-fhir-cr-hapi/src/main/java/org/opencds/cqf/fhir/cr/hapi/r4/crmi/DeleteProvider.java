package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.fhir.cr.hapi.IDeleteServiceFactory;

public class DeleteProvider {

    private final IDeleteServiceFactory r4DeleteServiceFactory;

    public DeleteProvider(IDeleteServiceFactory r4DeleteServiceFactory) {
        this.r4DeleteServiceFactory = r4DeleteServiceFactory;
    }

    /**
     * Deletes an existing artifact if it has status Retired.
     *
     * @param id              the {@link IdType IdType}, always an argument for instance level operations
     * @return A transaction {@link Bundle Bundle} result of the deleted resources
     */
    @Operation(name = "$delete", idempotent = true, global = true, type = MetadataResource.class)
    @Description(shortDefinition = "$delete", value = "Delete a retired artifact")
    public Bundle deleteOperation(@IdParam IdType id, RequestDetails requestDetails) throws FHIRException {
        return r4DeleteServiceFactory.create(requestDetails).delete(id);
    }
}
