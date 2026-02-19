package org.opencds.cqf.fhir.cr.hapi.r4.questionnaireresponse;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.opencds.cqf.fhir.cr.hapi.common.IQuestionnaireResponseProcessorFactory;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class QuestionnaireResponseExtractProvider {
    private final IQuestionnaireResponseProcessorFactory questionnaireResponseProcessorFactory;

    public QuestionnaireResponseExtractProvider(
            IQuestionnaireResponseProcessorFactory questionnaireResponseProcessorFactory) {
        this.questionnaireResponseProcessorFactory = questionnaireResponseProcessorFactory;
    }

    /**
     * Implements the <a href="http://build.fhir.org/ig/HL7/sdc/OperationDefinition-QuestionnaireResponse-extract.html">$extract</a>
     * operation found in the
     * <a href="http://build.fhir.org/ig/HL7/sdc/index.html">Structured Data Capture (SDC) IG</a>.
     *
     * @param id                    The id of the QuestionnaireResponse to extract data from.
     * @param questionnaire         The Questionnaire associated with the QuestionnaireResponse. Used if the server might not have access to the Questionnaire
     * @param parameters            Any input parameters defined in libraries referenced by the Questionnaire.
     * @param data                  Data to be made available during CQL evaluation.
     * @param requestDetails        The details (such as tenant) of this request. Usually
     *                                 autopopulated HAPI.
     * @return The resulting FHIR resource produced after extracting data. This will either be a single resource or a Transaction Bundle that contains multiple resources.
     */
    @Operation(name = ProviderConstants.CR_OPERATION_EXTRACT, idempotent = true, type = QuestionnaireResponse.class)
    public IBaseBundle extract(
            @IdParam IdType id,
            @OperationParam(name = "questionnaire") Questionnaire questionnaire,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return questionnaireResponseProcessorFactory
                .create(requestDetails)
                .extract(
                        Eithers.forLeft(id),
                        questionnaire == null ? null : Eithers.forRight(questionnaire),
                        parameters,
                        data,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue());
    }

    /**
     * Implements the <a href="http://build.fhir.org/ig/HL7/sdc/OperationDefinition-QuestionnaireResponse-extract.html">$extract</a>
     * operation found in the
     * <a href="http://build.fhir.org/ig/HL7/sdc/index.html">Structured Data Capture (SDC) IG</a>.
     *
     * @param questionnaireResponse The QuestionnaireResponse to extract data from. Used when the operation is invoked at the 'type' level.
     * @param questionnaire         The Questionnaire associated with the QuestionnaireResponse. Used if the server might not have access to the Questionnaire
     * @param parameters            Any input parameters defined in libraries referenced by the Questionnaire.
     * @param data                  Data to be made available during CQL evaluation.
     * @param requestDetails        The details (such as tenant) of this request. Usually
     *                                 autopopulated HAPI.
     * @return The resulting FHIR resource produced after extracting data. This will either be a single resource or a Transaction Bundle that contains multiple resources.
     */
    @Operation(name = ProviderConstants.CR_OPERATION_EXTRACT, idempotent = true, type = QuestionnaireResponse.class)
    public IBaseBundle extract(
            @OperationParam(name = "questionnaire-response") QuestionnaireResponse questionnaireResponse,
            @OperationParam(name = "questionnaire") Questionnaire questionnaire,
            @OperationParam(name = "parameters") Parameters parameters,
            @OperationParam(name = "useServerData") BooleanType useServerData,
            @OperationParam(name = "data") Bundle data,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        return questionnaireResponseProcessorFactory
                .create(requestDetails)
                .extract(
                        Eithers.forRight(questionnaireResponse),
                        questionnaire == null ? null : Eithers.for2(null, questionnaire),
                        parameters,
                        data,
                        useServerData == null ? Boolean.TRUE : useServerData.booleanValue());
    }
}
