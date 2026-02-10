package org.opencds.cqf.fhir.cr.hapi.r4.questionnaire;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;
import static org.opencds.cqf.fhir.utility.EndpointHelper.getEndpoint;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StringType;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class QuestionnairePackageProvider {
    private final IQuestionnaireProcessorFactory questionnaireProcessorFactory;
    private final FhirVersionEnum fhirVersion;

    public QuestionnairePackageProvider(IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        this.questionnaireProcessorFactory = questionnaireProcessorFactory;
        fhirVersion = FhirVersionEnum.R4;
    }

    /**
     * Implements a $package operation following the <a href="https://build.fhir.org/ig/HL7/crmi-ig/branches/master/packaging.html">CRMI IG</a>.
     *
     * @param id the id of the Resource.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param usePut the boolean value to determine if the Bundle returned uses PUT or POST request methods.  Defaults to false.
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a Bundle containing the ValueSet and all related CodeSystem and ValueSet resources
     */
    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Questionnaire.class)
    public Bundle packageQuestionnaire(
            @IdParam IdType id,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "usePut") BooleanType usePut,
            RequestDetails requestDetails) {
        return (Bundle) questionnaireProcessorFactory
                .create(requestDetails)
                .packageQuestionnaire(
                        Eithers.forMiddle3(id),
                        packageParameters(
                                fhirVersion,
                                getEndpoint(fhirVersion, terminologyEndpoint),
                                usePut == null ? Boolean.FALSE : usePut.booleanValue()));
    }

    /**
     * Implements a $package operation following the <a href="https://build.fhir.org/ig/HL7/crmi-ig/branches/master/packaging.html">CRMI IG</a>.
     *
     * @param id the id of the Resource.
     * @param canonical the canonical identifier for the Resource (optionally version-specific).
     * @param url canonical URL of the Resource when invoked at the resource type level. This is exclusive with the id and canonical parameters.
     * @param version version of the Resource when invoked at the resource type level. This is exclusive with the id and canonical parameters.
     * @param terminologyEndpoint the FHIR {@link Endpoint} Endpoint resource or url to use to access terminology (i.e. valuesets, codesystems, naming systems, concept maps, and membership testing) referenced by the Resource. If no terminology endpoint is supplied, the evaluation will attempt to use the server on which the operation is being performed as the terminology server.
     * @param usePut the boolean value to determine if the Bundle returned uses PUT or POST request methods.  Defaults to false.
     * @param requestDetails the details (such as tenant) of this request. Usually autopopulated by HAPI.
     * @return a Bundle containing the ValueSet and all related CodeSystem and ValueSet resources
     */
    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Questionnaire.class)
    public Bundle packageQuestionnaire(
            @OperationParam(name = "id") StringType id,
            @OperationParam(name = "canonical") Parameters.ParametersParameterComponent canonical,
            @OperationParam(name = "url") Parameters.ParametersParameterComponent url,
            @OperationParam(name = "version") StringType version,
            @OperationParam(name = "terminologyEndpoint") Parameters.ParametersParameterComponent terminologyEndpoint,
            @OperationParam(name = "usePut") BooleanType usePut,
            RequestDetails requestDetails) {
        return (Bundle) questionnaireProcessorFactory
                .create(requestDetails)
                .packageQuestionnaire(
                        Eithers.for3(
                                getCanonicalType(fhirVersion, canonical, url, version),
                                getIdType(fhirVersion, "Questionnaire", id),
                                null),
                        packageParameters(
                                fhirVersion,
                                getEndpoint(fhirVersion, terminologyEndpoint),
                                usePut == null ? Boolean.FALSE : usePut.booleanValue()));
    }
}
