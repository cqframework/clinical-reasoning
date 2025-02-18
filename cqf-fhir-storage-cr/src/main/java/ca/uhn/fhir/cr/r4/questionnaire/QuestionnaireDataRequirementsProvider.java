package ca.uhn.fhir.cr.r4.questionnaire;

import static ca.uhn.fhir.cr.common.CanonicalHelper.getCanonicalType;
import static ca.uhn.fhir.cr.common.IdHelper.getIdType;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.cr.common.IQuestionnaireProcessorFactory;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.opencds.cqf.fhir.utility.monad.Eithers;

public class QuestionnaireDataRequirementsProvider {
    private final IQuestionnaireProcessorFactory questionnaireFactory;

    public QuestionnaireDataRequirementsProvider(IQuestionnaireProcessorFactory theQuestionnaireFactory) {
        questionnaireFactory = theQuestionnaireFactory;
    }

    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = Questionnaire.class)
    public IBaseResource getDataRequirements(
            @IdParam IdType id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return questionnaireFactory
                .create(requestDetails)
                .dataRequirements(Eithers.for3(canonicalType, id, null), null);
    }

    @Operation(name = ProviderConstants.CR_OPERATION_DATAREQUIREMENTS, idempotent = true, type = Questionnaire.class)
    public IBaseResource getDataRequirements(
            @OperationParam(name = "id") String id,
            @OperationParam(name = "canonical") String canonical,
            @OperationParam(name = "url") String url,
            @OperationParam(name = "version") String version,
            RequestDetails requestDetails)
            throws InternalErrorException, FHIRException {
        IIdType idToUse = getIdType(FhirVersionEnum.R4, "Questionnaire", id);
        CanonicalType canonicalType = getCanonicalType(FhirVersionEnum.R4, canonical, url, version);
        return questionnaireFactory
                .create(requestDetails)
                .dataRequirements(Eithers.for3(canonicalType, idToUse, null), null);
    }
}
