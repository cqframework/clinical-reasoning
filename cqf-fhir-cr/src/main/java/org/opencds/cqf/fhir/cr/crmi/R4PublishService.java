package org.opencds.cqf.fhir.cr.crmi;

import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.opencds.cqf.fhir.cr.common.PublishProcessor;

public class R4PublishService {

    private final IRepository repository;
    private final PublishProcessor publishProcessor;

    public R4PublishService(IRepository repository) {
        this.repository = repository;
        this.publishProcessor = new PublishProcessor(repository);
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
     * @param bundle  A transaction Bundle conforming to CRMIPublishableBundle profile
     * @return        Transaction response Bundle
     * @throws FHIRException if bundle validation fails
     */
    public Bundle publishBundle(Bundle bundle) throws FHIRException {
        return (Bundle) publishProcessor.publishBundle(bundle);
    }
}
