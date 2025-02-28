package org.opencds.cqf.fhir.cr.hapi.dstu3.questionnaire;

import static org.opencds.cqf.fhir.cr.hapi.common.CanonicalHelper.getCanonicalType;
import static org.opencds.cqf.fhir.cr.hapi.common.IdHelper.getIdType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class QuestionnairePackageProvider {
    private final IQuestionnaireProcessorFactory questionnaireProcessorFactory;

    public QuestionnairePackageProvider(IQuestionnaireProcessorFactory questionnaireProcessorFactory) {
        this.questionnaireProcessorFactory = questionnaireProcessorFactory;
    }

    /**
     * Implements a $package operation following the <a href=
     * "https://build.fhir.org/ig/HL7/crmi-ig/branches/master/packaging.html">CRMI IG</a>.
     *
     * @param id             The id of the Questionnaire.
     * @param canonical         The canonical identifier for the Questionnaire (optionally version-specific).
     * @param url            Canonical URL of the Questionnaire when invoked at the resource type level. This is exclusive with the questionnaire and canonical parameters.
     * @param version        Version of the Questionnaire when invoked at the resource type level. This is exclusive with the questionnaire and canonical parameters.
     * @Param isPut			A boolean value to determine if the Bundle returned uses PUT or POST request methods.  Defaults to false.
     * @param requestDetails The details (such as tenant) of this request. Usually
     *                          autopopulated by HAPI.
     * @return A Bundle containing the Questionnaire and all related Library, CodeSystem and ValueSet resources
     */
    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Questionnaire.class)
    public Bundle packageQuestionnaire(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "usePut") BooleanType isPut,
            RequestDetails requestDetails) {
        StringType canonicalType = getCanonicalType(FhirVersionEnum.DSTU3, canonical, url, version);
        return (Bundle) questionnaireProcessorFactory
                .create(requestDetails)
                .packageQuestionnaire(
                        Eithers.for3(canonicalType, id, null), isPut == null ? Boolean.FALSE : isPut.booleanValue());
    }

    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Questionnaire.class)
    public Bundle packageQuestionnaire(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "usePut") BooleanType isPut,
            RequestDetails requestDetails) {
        IIdType idToUse = getIdType(FhirVersionEnum.DSTU3, "Questionnaire", id);
        StringType canonicalType = getCanonicalType(FhirVersionEnum.DSTU3, canonical, url, version);
        return (Bundle) questionnaireProcessorFactory
                .create(requestDetails)
                .packageQuestionnaire(
                        Eithers.for3(canonicalType, idToUse, null),
                        isPut == null ? Boolean.FALSE : isPut.booleanValue());
    }
}
