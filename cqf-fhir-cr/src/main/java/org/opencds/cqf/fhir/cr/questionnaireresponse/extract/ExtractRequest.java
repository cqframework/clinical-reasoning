package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.utility.Constants;

public class ExtractRequest implements IQuestionnaireRequest {
    private final IBaseResource questionnaireResponse;
    private final IBaseResource questionnaire;
    private final IIdType subjectId;
    private final IBaseParameters parameters;
    private final IBaseBundle data;
    private final boolean useServerData;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirContext fhirContext;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;
    private IBaseOperationOutcome operationOutcome;

    public ExtractRequest(
            IBaseResource questionnaireResponse,
            IBaseResource questionnaire,
            IIdType subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            boolean useServerData,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(questionnaireResponse, "expected non-null value for questionnaireResponse");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        this.questionnaireResponse = questionnaireResponse;
        this.questionnaire = questionnaire;
        this.subjectId = subjectId;
        this.parameters = parameters;
        this.data = bundle;
        this.useServerData = useServerData;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.fhirContext = this.libraryEngine.getRepository().fhirContext();
        this.fhirVersion = questionnaireResponse.getStructureFhirVersionEnum();
        this.defaultLibraryUrl = "";
    }

    public IBaseResource getQuestionnaireResponse() {
        return questionnaireResponse;
    }

    public boolean hasQuestionnaire() {
        return questionnaire != null;
    }

    public IBaseResource getQuestionnaire() {
        return questionnaire;
    }

    public IBaseBackboneElement getQuestionnaireItem(IBaseBackboneElement item) {
        return hasQuestionnaire() ? getQuestionnaireItem(item, getItems(getQuestionnaire())) : null;
    }

    public IBaseBackboneElement getQuestionnaireItem(IBaseBackboneElement item, List<IBaseBackboneElement> qItems) {
        return qItems != null
                ? qItems.stream()
                        .filter(i -> getItemLinkId(i).equals(getItemLinkId(item)))
                        .findFirst()
                        .orElse(null)
                : null;
    }

    public boolean isDefinitionItem(IBaseBackboneElement item, IBaseBackboneElement qrItem) {
        return StringUtils.isNotBlank(resolvePathString(item == null ? qrItem : item, "definition"));
    }

    public IBaseExtension<?, ?> getItemExtractionContext() {
        var qrExt = getExtensions(questionnaireResponse).stream()
                .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT))
                .findFirst()
                .orElse(null);
        if (qrExt != null) {
            return qrExt;
        }
        return questionnaire == null
                ? null
                : getExtensions(questionnaire).stream()
                        .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT))
                        .findFirst()
                        .orElse(null);
    }

    public String getExtractId() {
        return "extract-" + questionnaireResponse.getIdElement().getIdPart();
    }

    public FhirContext getFhirContext() {
        return fhirContext;
    }

    @Override
    public String getOperationName() {
        return "extract";
    }

    @Override
    public IIdType getSubjectId() {
        return subjectId;
    }

    @Override
    public IBaseBundle getData() {
        return data;
    }

    @Override
    public boolean getUseServerData() {
        return useServerData;
    }

    @Override
    public IBaseParameters getParameters() {
        return parameters;
    }

    @Override
    public LibraryEngine getLibraryEngine() {
        return libraryEngine;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    @Override
    public FhirVersionEnum getFhirVersion() {
        return fhirVersion;
    }

    @Override
    public String getDefaultLibraryUrl() {
        return defaultLibraryUrl;
    }

    @Override
    public IBaseOperationOutcome getOperationOutcome() {
        return operationOutcome;
    }

    @Override
    public void setOperationOutcome(IBaseOperationOutcome operationOutcome) {
        this.operationOutcome = operationOutcome;
    }
}
