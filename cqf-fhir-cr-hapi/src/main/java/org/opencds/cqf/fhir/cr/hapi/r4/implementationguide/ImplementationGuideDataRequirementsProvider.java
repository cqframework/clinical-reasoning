package org.opencds.cqf.fhir.cr.hapi.r4.implementationguide;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.IImplementationGuideProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ImplementationGuideDataRequirementsProvider {
    private final IImplementationGuideProcessorFactory implementationGuideProcessorFactory;

    public ImplementationGuideDataRequirementsProvider(
            IImplementationGuideProcessorFactory implementationGuideProcessorFactory) {
        this.implementationGuideProcessorFactory = implementationGuideProcessorFactory;
    }

    @Operation(
            name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS,
            idempotent = true,
            type = ImplementationGuide.class)
    public IBaseResource getDataRequirements(
            @IdParam IdType id,
            @OperationParam(name = "implementationGuide") ImplementationGuide implementationGuide,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "persistDependencies") BooleanType persistDependencies,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        StringType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .dataRequirements(
                        Eithers.for3(canonicalType, id, implementationGuide),
                        null,
                        persistDependencies != null && persistDependencies.getValue());
    }

    @Operation(
            name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS,
            idempotent = true,
            type = ImplementationGuide.class)
    public IBaseResource getDataRequirements(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "implementationGuide") ImplementationGuide implementationGuide,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "persistDependencies") BooleanType persistDependencies,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        IIdType idToUse = getIdType(FhirVersionEnum.R4, "ImplementationGuide", id);
        StringType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .dataRequirements(
                        Eithers.for3(canonicalType, idToUse, implementationGuide),
                        null,
                        persistDependencies != null && persistDependencies.getValue());
    }
}
