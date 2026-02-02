package org.opencds.cqf.fhir.cr.hapi.r4.crmi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.opencds.cqf.fhir.cr.hapi.r4.IPublishServiceFactory;

public class PublishProvider {

    private final IPublishServiceFactory r4PublishServiceFactory;

    public PublishProvider(IPublishServiceFactory r4PublishServiceFactory) {
        this.r4PublishServiceFactory = r4PublishServiceFactory;
    }

    /**
     * Publishes a knowledge artifact bundle per CRMI specification.
     * The bundle must conform to the CRMIPublishableBundle profile:
     * <ul>
     *   <li>Bundle type must be "transaction"</li>
     *   <li>First entry must contain an ImplementationGuide resource</li>
     *   <li>All entries must have proper transaction request information</li>
     * </ul>
     *
     * @param bundle          A transaction Bundle conforming to CRMIPublishableBundle profile
     * @param requestDetails  The {@link RequestDetails RequestDetails}
     * @return                Transaction response Bundle
     */
    @Operation(
            name = "$publish",
            idempotent = false,
            canonicalUrl = "http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-publish")
    @Description(shortDefinition = "$publish", value = "Publishes a knowledge artifact bundle per CRMI specification.")
    public Bundle publish(@OperationParam(name = "bundle") Bundle bundle, RequestDetails requestDetails) {
        return r4PublishServiceFactory.create(requestDetails).publishBundle(bundle);
    }
}
