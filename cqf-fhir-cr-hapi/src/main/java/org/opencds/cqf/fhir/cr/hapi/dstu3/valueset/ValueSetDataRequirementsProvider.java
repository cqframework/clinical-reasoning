package org.opencds.cqf.fhir.cr.hapi.dstu3.valueset;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ValueSetDataRequirementsProvider {
    private final IValueSetProcessorFactory valueSetFactory;

    public ValueSetDataRequirementsProvider(IValueSetProcessorFactory valueSetFactory) {
        this.valueSetFactory = valueSetFactory;
    }

    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = ValueSet.class)
    public IBaseResource getDataRequirements(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        StringType canonicalType = getCanonicalType(FhirVersionEnum.DSTU3, canonical, url, version);
        return valueSetFactory.create(requestDetails).dataRequirements(Eithers.for3(canonicalType, id, null), null);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = ValueSet.class)
    public IBaseResource getDataRequirements(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        IdType idToUse = id == null ? null : new IdType("ValueSet", id);
        StringType canonicalType = getCanonicalType(FhirVersionEnum.DSTU3, canonical, url, version);
        return valueSetFactory
                .create(requestDetails)
                .dataRequirements(Eithers.for3(canonicalType, idToUse, null), null);
    }
}
