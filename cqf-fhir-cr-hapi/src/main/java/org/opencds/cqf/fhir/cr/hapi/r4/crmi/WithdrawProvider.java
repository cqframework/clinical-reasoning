package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MetadataResource;
import org.opencds.cqf.fhir.cr.hapi.r4.IWithdrawServiceFactory;

public class WithdrawProvider {

    private final IWithdrawServiceFactory r4WithdrawServiceFactory;

    public WithdrawProvider(IWithdrawServiceFactory r4WithdrawServiceFactory) {
        this.r4WithdrawServiceFactory = r4WithdrawServiceFactory;
    }

    /**
     * Withdraws an existing artifact if it has status Draft.
     *
     * @param id                the {@link IdType IdType}, always an argument for instance level operations
     * @param requestDetails    the {@link RequestDetails RequestDetails}
     * @return A transaction bundle result of the withdrawn resources
     */
    @Operation(name = "$withdraw", idempotent = true, global = true, type = MetadataResource.class)
    @Description(shortDefinition = "$withdraw", value = "Withdraw an existing draft artifact")
    public Bundle withdrawOperation(@IdParam IdType id, RequestDetails requestDetails) {
        return r4WithdrawServiceFactory.create(requestDetails).withdraw(id);
    }

}
