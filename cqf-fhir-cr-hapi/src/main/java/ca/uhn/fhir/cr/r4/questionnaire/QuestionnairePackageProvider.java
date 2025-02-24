package ca.uhn.fhir.cr.r4.questionnaire;

import static ca.uhn.fhir.cr.common.CanonicalHelper.getCanonicalType;
import static ca.uhn.fhir.cr.common.IdHelper.getIdType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.IQuestionnaireProcessorFactory;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;
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
     * @param canonical      The canonical identifier for the Questionnaire (optionally version-specific).
     * @param url            Canonical URL of the Questionnaire when invoked at the resource type level. This is exclusive with the questionnaire and canonical parameters.
     * @param version        Version of the Questionnaire when invoked at the resource type level. This is exclusive with the questionnaire and canonical parameters.
     * @Param qisPut			A boolean value to determine if the Bundle returned uses PUT or POST request methods.  Defaults to false.
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
            @OperationParam(name = "usePut") BooleanType qisPut,
            RequestDetails requestDetails) {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return (Bundle) questionnaireProcessorFactory
                .create(requestDetails)
                .packageQuestionnaire(
                        Eithers.for3(canonicalType, id, null), qisPut == null ? Boolean.FALSE : qisPut.booleanValue());
    }

    @Operation(name = ProviderConstants.CR_OPERATION_PACKAGE, idempotent = true, type = Questionnaire.class)
    public Bundle packageQuestionnaire(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            @OperationParam(name = "usePut") BooleanType qisPut,
            RequestDetails requestDetails) {
        IIdType idToUse = getIdType(FhirVersionEnum.R4, "Questionnaire", id);
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return (Bundle) questionnaireProcessorFactory
                .create(requestDetails)
                .packageQuestionnaire(
                        Eithers.for3(canonicalType, idToUse, null),
                        qisPut == null ? Boolean.FALSE : qisPut.booleanValue());
    }
}
