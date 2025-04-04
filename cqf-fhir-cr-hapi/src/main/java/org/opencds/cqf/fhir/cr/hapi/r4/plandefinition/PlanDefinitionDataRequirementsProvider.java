package org.opencds.cqf.fhir.cr.hapi.r4.plandefinition;

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
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.IPlanDefinitionProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class PlanDefinitionDataRequirementsProvider {
    private final IPlanDefinitionProcessorFactory planDefinitionProcessorFactory;

    public PlanDefinitionDataRequirementsProvider(IPlanDefinitionProcessorFactory planDefinitionProcessorFactory) {
        this.planDefinitionProcessorFactory = planDefinitionProcessorFactory;
    }

    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = PlanDefinition.class)
    public IBaseResource getDataRequirements(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        StringType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .dataRequirements(Eithers.for3(canonicalType, id, null), null);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = PlanDefinition.class)
    public IBaseResource getDataRequirements(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        IIdType idToUse = getIdType(FhirVersionEnum.R4, "PlanDefinition", id);
        StringType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return planDefinitionProcessorFactory
                .create(requestDetails)
                .dataRequirements(Eithers.for3(canonicalType, idToUse, null), null);
    }
}
