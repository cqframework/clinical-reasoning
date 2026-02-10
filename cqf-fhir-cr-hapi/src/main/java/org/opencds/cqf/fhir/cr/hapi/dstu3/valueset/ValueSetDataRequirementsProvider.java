package org.opencds.cqf.fhir.cr.hapi.dstu3.valueset;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.ValueSet;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cr.hapi.common.IValueSetProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ValueSetDataRequirementsProvider {
    private final IValueSetProcessorFactory valueSetFactory;
    private final FhirVersionEnum fhirVersion;

    public ValueSetDataRequirementsProvider(IValueSetProcessorFactory valueSetFactory) {
        this.valueSetFactory = valueSetFactory;
        fhirVersion = FhirVersionEnum.DSTU3;
    }

    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = ValueSet.class)
    public IBaseResource getDataRequirements(@IdParam IdType id, RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return valueSetFactory.create(requestDetails).dataRequirements(Eithers.forMiddle3(id), null);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = ValueSet.class)
    public IBaseResource getDataRequirements(
            @OperationParam(name = "id") StringType id,
            @OperationParam(name = "canonical") ParametersParameterComponent canonical,
            @OperationParam(name = "url") ParametersParameterComponent url,
            @OperationParam(name = "version") StringType version,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return valueSetFactory
                .create(requestDetails)
                .dataRequirements(
                        Eithers.for3(
                                getCanonicalType(fhirVersion, canonical, url, version),
                                getIdType(fhirVersion, "ValueSet", id),
                                null),
                        null);
    }
}
