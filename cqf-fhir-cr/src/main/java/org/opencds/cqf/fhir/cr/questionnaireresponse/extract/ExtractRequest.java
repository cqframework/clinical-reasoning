package org.opencds.cqf.fhir.cr.questionnaireresponse.extract;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.cr.common.IInputParameterResolver.createResolver;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IItemComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseAdapter;

@SuppressWarnings("UnstableApiUsage")
public class ExtractRequest implements IQuestionnaireRequest {
    private final IQuestionnaireResponseAdapter questionnaireResponseAdapter;
    private final IQuestionnaireAdapter questionnaireAdapter;
    private final IIdType subjectId;
    private final IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirContext fhirContext;
    private final FhirVersionEnum fhirVersion;
    private final Map<String, String> referencedLibraries;
    private final IInputParameterResolver inputParameterResolver;
    private IBaseOperationOutcome operationOutcome;

    public ExtractRequest(
            IBaseResource questionnaireResponse,
            IBaseResource questionnaire,
            IIdType subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver,
            IInputParameterResolver inputParameterResolver) {
        checkNotNull(questionnaireResponse, "expected non-null value for questionnaireResponse");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        fhirVersion = questionnaireResponse.getStructureFhirVersionEnum();
        questionnaireResponseAdapter = getAdapterFactory().createQuestionnaireResponse(questionnaireResponse);
        questionnaireAdapter =
                questionnaire == null ? null : getAdapterFactory().createQuestionnaire(questionnaire);
        this.subjectId = subjectId;
        this.data = bundle;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.inputParameterResolver = inputParameterResolver != null
                ? inputParameterResolver
                : createResolver(libraryEngine.getRepository(), this.subjectId, null, null, parameters, this.data);
        fhirContext = this.libraryEngine.getRepository().fhirContext();
        referencedLibraries = Map.of();
    }

    public IBaseResource getQuestionnaireResponse() {
        return questionnaireResponseAdapter.get();
    }

    public IQuestionnaireResponseAdapter getQuestionnaireResponseAdapter() {
        return questionnaireResponseAdapter;
    }

    public boolean hasQuestionnaire() {
        return questionnaireAdapter != null;
    }

    public IBaseResource getQuestionnaire() {
        return hasQuestionnaire() ? questionnaireAdapter.get() : null;
    }

    public IQuestionnaireAdapter getQuestionnaireAdapter() {
        return questionnaireAdapter;
    }

    public IItemComponentAdapter getQuestionnaireItem(IItemComponentAdapter item) {
        return hasQuestionnaire()
                ? getQuestionnaireItem(item, getQuestionnaireAdapter().getItem())
                : null;
    }

    public <T extends IItemComponentAdapter> IItemComponentAdapter getQuestionnaireItem(
            T item, List<? extends IItemComponentAdapter> qItems) {
        return qItems != null
                ? qItems.stream()
                        .filter(i -> i.getLinkId().equals(item.getLinkId()))
                        .findFirst()
                        .orElse(null)
                : null;
    }

    public boolean isDefinitionItem(ItemPair item) {
        var targetItem = item.getItem() == null ? item.getResponseItem() : item.getItem();
        return targetItem.hasExtension(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                || targetItem.hasExtension(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT)
                || StringUtils.isNotBlank(targetItem.getDefinition());
    }

    @SuppressWarnings("unchecked")
    public <T extends IBaseExtension<?, ?>> T getDefinitionExtract() {
        var qrExt = questionnaireResponseAdapter.getExtension().stream()
                .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                        || e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT))
                .findFirst()
                .orElse(null);
        if (qrExt != null) {
            return (T) qrExt;
        }
        return questionnaireAdapter == null
                ? null
                : (T) questionnaireAdapter.getExtension().stream()
                        .filter(e -> e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_ITEM_EXTRACTION_CONTEXT)
                                || e.getUrl().equals(Constants.SDC_QUESTIONNAIRE_DEFINITION_EXTRACT))
                        .findFirst()
                        .orElse(null);
    }

    public String getExtractId() {
        return "extract-" + questionnaireResponseAdapter.get().getIdElement().getIdPart();
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
    public IBase getContextVariable() {
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
    public Map<String, String> getReferencedLibraries() {
        return referencedLibraries;
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
