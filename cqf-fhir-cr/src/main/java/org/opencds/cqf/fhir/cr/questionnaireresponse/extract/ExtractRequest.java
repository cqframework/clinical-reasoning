package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver.createResolver;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;

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
    private final IInputParameterResolver inputParameterResolver;
    private IBaseOperationOutcome operationOutcome;
    private IQuestionnaireAdapter questionnaireAdapter;

    public ExtractRequest(
            IBaseResource questionnaireResponse,
            IBaseResource questionnaire,
            IIdType subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            boolean useServerData,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver,
            IInputParameterResolver inputParameterResolver) {
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
        this.inputParameterResolver = inputParameterResolver != null
                ? inputParameterResolver
                : createResolver(
                        libraryEngine.getRepository(),
                        this.subjectId,
                        null,
                        null,
                        this.parameters,
                        this.useServerData,
                        this.data);
        fhirContext = this.libraryEngine.getRepository().fhirContext();
        fhirVersion = this.questionnaireResponse.getStructureFhirVersionEnum();
        defaultLibraryUrl = "";
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

    public IQuestionnaireAdapter getQuestionnaireAdapter() {
        if (questionnaireAdapter == null && questionnaire != null) {
            questionnaireAdapter = (IQuestionnaireAdapter)
                    getAdapterFactory().createKnowledgeArtifactAdapter((IDomainResource) questionnaire);
        }
        return questionnaireAdapter;
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

    public boolean isDefinitionItem(ItemPair item) {
        var targetItem = item.getItem() == null ? item.getResponseItem() : item.getItem();
        return hasExtension(targetItem, Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                || hasExtension(targetItem, Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT)
                || StringUtils.isNotBlank(resolvePathString(targetItem, "definition"));
    }

    @SuppressWarnings("unchecked")
    public <T extends IBaseExtension<?, ?>> T getDefinitionExtract() {
        var qrExt = getExtensions(questionnaireResponse).stream()
                .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                        || e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT))
                .findFirst()
                .orElse(null);
        if (qrExt != null) {
            return (T) qrExt;
        }
        return questionnaire == null
                ? null
                : (T) getExtensions(questionnaire).stream()
                        .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                                || e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT))
                        .findFirst()
                        .orElse(null);
    }

    public String getExtractId() {
        return "extract-" + questionnaireResponse.getIdElement().getIdPart();
    }

    @Override
    public FhirContext getFhirContext() {
        return fhirContext;
    }

    @Override
    public String getOperationName() {
        return "extract";
    }

    @Override
    public IBase getContext() {
        return getQuestionnaireResponse();
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
        return inputParameterResolver.getParameters();
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
