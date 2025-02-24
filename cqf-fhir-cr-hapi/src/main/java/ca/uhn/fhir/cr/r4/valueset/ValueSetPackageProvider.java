package ca.uhn.fhir.cr.r4.valueset;

import static ca.uhn.fhir.cr.common.CanonicalHelper.getCanonicalType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.IValueSetProcessorFactory;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ValueSet;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class ValueSetPackageProvider {
    private final IValueSetProcessorFactory valueSetProcessorFactory;

    public ValueSetPackageProvider(IValueSetProcessorFactory valueSetProcessorFactory) {
        this.valueSetProcessorFactory = valueSetProcessorFactory;
    }

    /**
     * Implements a $package operation following the <a href=
     * "https://build.fhir.org/ig/HL7/crmi-ig/branches/master/packaging.html">CRMI IG</a>.
     *
     * @param id             The id of the ValueSet.
     * @param canonical      The canonical identifier for the ValueSet (optionally version-specific).
     * @param url            Canonical URL of the ValueSet when invoked at the resource type level. This is exclusive with the ValueSet and canonical parameters.
     * @param version        Version of the ValueSet when invoked at the resource type level. This is exclusive with the ValueSet and canonical parameters.
     * @Param isPut			A boolean value to determine if the Bundle returned uses PUT or POST request methods.  Defaults to false.
     * @param requestDetails The details (such as tenant) of this request. Usually
     *                          autopopulated by HAPI.
     * @return A Bundle containing the ValueSet and all related CodeSystem and ValueSet resources
     */
    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = ValueSet.class)
    public Bundle packageValueSet(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "usePut") BooleanType isPut,
            RequestDetails requestDetails) {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return (Bundle) valueSetProcessorFactory
                .create(requestDetails)
                .packageValueSet(
                        Eithers.for3(canonicalType, id, null), isPut == null ? Boolean.FALSE : isPut.booleanValue());
    }

    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = ValueSet.class)
    public Bundle packageValueSet(
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "usePut") BooleanType isPut,
            RequestDetails requestDetails) {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return (Bundle) valueSetProcessorFactory
                .create(requestDetails)
                .packageValueSet(
                        Eithers.for3(canonicalType, null, null), isPut == null ? Boolean.FALSE : isPut.booleanValue());
    }
}
