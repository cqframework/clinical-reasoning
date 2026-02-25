package org.opencds.cqf.fhir.cr.hapi.r4.ecr;

import static org.opencds.cqf.fhir.cr.hapi.common.ParameterHelper.getStringValue;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.ecr.FhirResourceExistsException;
import org.opencds.cqf.fhir.cr.hapi.r4.IERSDV2ImportServiceFactory;

public class ERSDTransformProvider {
    private final IERSDV2ImportServiceFactory ersdv2ImportServiceFactory;

    public ERSDTransformProvider(IERSDV2ImportServiceFactory ersdv2ImportServiceFactory) {
        this.ersdv2ImportServiceFactory = ersdv2ImportServiceFactory;
    }

    /**
     * Implements the $ersd-v2-import operation which loads an active (released)
     * eCR Version 2.1.1 (http://hl7.org/fhir/us/ecr/ImplementationGuide/hl7.fhir.us.ecr|2.1.1) conformant
     * eRSD Bundle
     * and transforms it into and Value Set Manager authoring state
     *
     * @param requestDetails      the incoming request details
     * @param maybeBundle         the v2 bundle to import
     * @return the OperationOutcome
     */
    @Description(shortDefinition = "Imports a v2 ERSD bundle", value = "Imports a v2 ERSD bundle")
    @Operation(idempotent = true, name = "$ersd-v2-import")
    public OperationOutcome eRSDV2ImportOperation(
            RequestDetails requestDetails,
            @OperationParam(name = "bundle") IBaseResource maybeBundle,
            @OperationParam(name = "appAuthoritativeUrl") StringType appAuthoritativeUrl)
            throws UnprocessableEntityException, FhirResourceExistsException {
        return ersdv2ImportServiceFactory
                .create(requestDetails)
                .eRSDV2ImportOperation(maybeBundle, getStringValue(appAuthoritativeUrl));
    }
}
