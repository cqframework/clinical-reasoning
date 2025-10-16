package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.fhir.cr.hapi.IWithdrawServiceFactory;

public class WithdrawProvider {

    private final IWithdrawServiceFactory r4WithdrawServiceFactory;

    public WithdrawProvider(IWithdrawServiceFactory r4WithdrawServiceFactory) {
        this.r4WithdrawServiceFactory = r4WithdrawServiceFactory;
    }

    @Operation(name = "$withdraw", idempotent = true, global = true, type = MetadataResource.class)
    @Description(shortDefinition = "$withdraw", value = "Withdraw an existing draft artifact")
    public Bundle withdrawOperation(@IdParam IdType id, RequestDetails requestDetails) throws FHIRException {
        return r4WithdrawServiceFactory.create(requestDetails).withdraw(id);
    }
}
