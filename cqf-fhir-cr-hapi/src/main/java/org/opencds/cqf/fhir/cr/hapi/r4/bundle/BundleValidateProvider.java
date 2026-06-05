package org.opencds.cqf.fhir.cr.hapi.r4.bundle;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_OPERATION_VALIDATE;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.IBundleProcessorFactory;

public class BundleValidateProvider {

    private final IBundleProcessorFactory bundleProcessorFactory;

    public BundleValidateProvider(IBundleProcessorFactory bundleProcessorFactory) {
        this.bundleProcessorFactory = bundleProcessorFactory;
    }

    @Operation(name = CRMI_OPERATION_VALIDATE, idempotent = true, type = Bundle.class)
    @Description(shortDefinition = CRMI_OPERATION_VALIDATE, value = "Validate a bundle")
    public OperationOutcome validateOperation(
            RequestDetails requestDetails,
            @OperationParam(name = "bundle") Bundle bundle,
            @OperationParam(name = "mode") StringType mode,
            @OperationParam(name = "profile") StringType profile) {
        return (OperationOutcome) bundleProcessorFactory
                .create(requestDetails)
                .validate(bundle, getStringValue(mode), getStringValue(profile));
    }
}
