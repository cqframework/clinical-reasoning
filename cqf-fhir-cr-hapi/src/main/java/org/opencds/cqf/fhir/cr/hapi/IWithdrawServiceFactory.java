package org.opencds.cqf.fhir.cr.hapi;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.opencds.cqf.fhir.cr.crmi.R4WithdrawService;

@FunctionalInterface
public interface IWithdrawServiceFactory {

    R4WithdrawService create(RequestDetails requestDetails);
}
