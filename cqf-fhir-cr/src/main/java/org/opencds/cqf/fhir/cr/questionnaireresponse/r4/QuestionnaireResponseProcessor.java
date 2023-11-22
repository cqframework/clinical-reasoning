package org.opencds.cqf.fhir.cr.questionnaireresponse.r4;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Enumerations.FHIRAllTypes;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cr.questionnaireresponse.BaseQuestionnaireResponseProcessor;
import org.opencds.cqf.fhir.utility.Constants;

public class QuestionnaireResponseProcessor extends BaseQuestionnaireResponseProcessor<QuestionnaireResponse> {
    ProcessorService processorService;

    public QuestionnaireResponseProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public QuestionnaireResponseProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        super(repository, evaluationSettings);
    }

    @Override
    public QuestionnaireResponse resolveQuestionnaireResponse(IIdType id, IBaseResource questionnaireResponse) {
        var baseQuestionnaireResponse = questionnaireResponse;
        if (baseQuestionnaireResponse == null && id != null) {
            baseQuestionnaireResponse = this.repository.read(QuestionnaireResponse.class, id);
        }

        return castOrThrow(
            baseQuestionnaireResponse,
            QuestionnaireResponse.class,
            "The QuestionnaireResponse passed to repository was not a valid instance of QuestionnaireResponse.class")
            .orElse(null);
    }

    @Override
    protected IBaseBundle createResourceBundle(
        QuestionnaireResponse questionnaireResponse, List<IBaseResource> resources) {
        var newBundle = new Bundle();
        newBundle.setId(new IdType(FHIRAllTypes.BUNDLE.toCode(), getExtractId(questionnaireResponse)));
        newBundle.setType(Bundle.BundleType.TRANSACTION);
        resources.forEach(resource -> {
            var request = new Bundle.BundleEntryRequestComponent();
            request.setMethod(Bundle.HTTPVerb.PUT);
            request.setUrl(resource.fhirType() + "/" + resource.getIdElement().getIdPart());
            var entry = new Bundle.BundleEntryComponent();
            entry.setResource((Resource) resource);
            entry.setRequest(request);
            newBundle.addEntry(entry);
        });

        return newBundle;
    }

    @Override
    protected void setup(QuestionnaireResponse questionnaireResponse) {
        patientId = questionnaireResponse.getSubject().getId();
        libraryUrl = questionnaireResponse.hasExtension(Constants.CQF_LIBRARY)
            ? ((CanonicalType) questionnaireResponse
            .getExtensionByUrl(Constants.CQF_LIBRARY)
            .getValue())
            .getValue()
            : null;
    }

    @Override
    public List<IBaseResource> processItems(QuestionnaireResponse questionnaireResponse) {
        return processorService.processItems(questionnaireResponse);
    }
}

