package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseBundle;

/**
 * Interface for CRMI $publish operation processor.
 *
 * <p>Per CRMI specification: http://hl7.org/fhir/uv/crmi/OperationDefinition/crmi-publish
 *
 * <p>The $publish operation processes a transaction bundle containing knowledge artifacts
 * and their metadata. The bundle must conform to the CRMIPublishableBundle profile:
 * <ul>
 *   <li>Bundle type must be "transaction"
 *   <li>First entry must contain an ImplementationGuide resource
 *   <li>All entries must have proper transaction request information
 * </ul>
 */
public interface IPublishProcessor extends IOperationProcessor {
    /**
     * Publishes a knowledge artifact bundle per CRMI specification.
     *
     * @param bundle A transaction Bundle conforming to CRMIPublishableBundle profile
     * @return Transaction response Bundle
     */
    IBaseBundle publishBundle(IBaseBundle bundle);
}
