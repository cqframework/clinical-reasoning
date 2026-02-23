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
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImplementationGuide;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.IImplementationGuideProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ImplementationGuideDataRequirements {
    private final IImplementationGuideProcessorFactory implementationGuideProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public ImplementationGuideDataRequirements(
            IImplementationGuideProcessorFactory implementationGuideProcessorFactory) {
        this.implementationGuideProcessorFactory = implementationGuideProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Implements the CRMI <a href="https://build.fhir.org/ig/HL7/crmi-ig/OperationDefinition-crmi-data-requirements.html">$data-requirements</a>
     * operation for ImplementationGuide resources.
     *
     * @param id the id of the ImplementationGuide resource
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a module-definition Library describing the data requirements
     */
    @Operation(
            name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS,
            idempotent = true,
            type = ImplementationGuide.class)
    public IBaseResource getDataRequirements(@IdParam IdType id, RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .dataRequirements(Eithers.forMiddle3(id), null);
    }

    /**
     * Implements the CRMI <a href="https://build.fhir.org/ig/HL7/crmi-ig/OperationDefinition-crmi-data-requirements.html">$data-requirements</a>
     * operation for ImplementationGuide resources.
     *
     * @param id the logical id of the ImplementationGuide resource to analyze
     * @param canonical a canonical reference to the ImplementationGuide resource
     * @param url canonical URL of the ImplementationGuide when invoked at the resource type level
     * @param version version of the ImplementationGuide when invoked at the resource type level
     * @param parameters any input parameters for the data requirements analysis
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a module-definition Library describing the data requirements
     */
    @Operation(
            name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS,
            idempotent = true,
            type = ImplementationGuide.class)
    public IBaseResource getDataRequirements(
            @OperationParam(name = "id") StringType id,
            @OperationParam(name = "canonical", typeName = "canonical") IPrimitiveType<String> canonical,
            @OperationParam(name = "url", typeName = "uri") IPrimitiveType<String> url,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "parameters", max = 1) Parameters parameters,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return implementationGuideProcessorFactory
                .create(requestDetails)
                .dataRequirements(
                        Eithers.for3(
                                getCanonicalType(fhirVersion, canonical, url, version),
                                getIdType(fhirVersion, "ImplementationGuide", id),
                                null),
                        parameters);
    }
}
