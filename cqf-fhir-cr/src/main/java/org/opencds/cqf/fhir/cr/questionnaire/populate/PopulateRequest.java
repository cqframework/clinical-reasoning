package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireResponseItemComponentAdapter;

public class PopulateRequest implements IQuestionnaireRequest {
    private final IQuestionnaireAdapter questionnaireAdapter;
    private final IQuestionnaireResponseAdapter questionnaireResponseAdapter;
    private final IIdType subjectId;
    private final List<IParametersParameterComponentAdapter> context;
    private final IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final Map<String, String> referencedLibraries;
    private final IInputParameterResolver inputParameterResolver;
    private IBase contextVariable;
    private IBaseOperationOutcome operationOutcome;

    public PopulateRequest(
            IBaseResource questionnaire,
            IIdType subjectId,
            List<? extends IBaseBackboneElement> context,
            IBaseExtension<?, ?> launchContext,
            // IBaseParameters parameters,
            IBaseBundle data,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(questionnaire, "expected non-null value for questionnaire");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        fhirVersion = questionnaire.getStructureFhirVersionEnum();
        questionnaireAdapter = (IQuestionnaireAdapter)
                getAdapterFactory().createKnowledgeArtifactAdapter((IDomainResource) questionnaire);
        this.context = context == null
                ? new ArrayList<>()
                : context.stream()
                        .map(c -> getAdapterFactory().createParametersParameter(c))
                        .collect(Collectors.toList());
        this.subjectId = getSubjectId(subjectId);
        this.data = data;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        var launchContexts = getExtensionsByUrl(questionnaireAdapter.get(), Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
        if (launchContext != null) {
            launchContexts.add(launchContext);
        }
        // if (parameters == null) {
        var parameters = (IBaseParameters) Resources.newBaseForVersion("Parameters", fhirVersion);
        // }
        getAdapterFactory().createParameters(parameters).addParameter("%questionnaire", questionnaireAdapter.get());
        questionnaireResponseAdapter = createQuestionnaireResponse();
        contextVariable = questionnaireResponseAdapter.get();
        referencedLibraries = questionnaireAdapter.getReferencedLibraries();
        inputParameterResolver = IInputParameterResolver.createResolver(
                libraryEngine.getRepository(), null, null, null, parameters, this.data, this.context, launchContexts);
    }

    @SuppressWarnings("unchecked")
    protected IIdType getSubjectId(IIdType subject) {
        var subjectContext = context.stream()
                .filter(c -> c.getPartValues("name").stream()
                        .anyMatch(p ->
                                ((IPrimitiveType<String>) p).getValueAsString().equals("patient")))
                .findFirst()
                .orElse(null);
        if (subjectContext == null && !context.isEmpty()) {
            subjectContext = context.get(0);
        }
        if (subjectContext != null) {
            var subjectContextValue =
                    subjectContext.getPartValues("content").stream().findFirst().orElse(null);
            if (subjectContextValue instanceof IBaseReference subjectRef) {
                return subjectRef.getReferenceElement();
            } else if (subjectContextValue instanceof IBaseResource subjectResource) {
                return subjectResource.getIdElement();
            }
        }
        if (subject != null) {
            return subject;
        }

        throw new IllegalArgumentException("Unable to determine subject from launch context.");
    }

    @Override
    public String getOperationName() {
        return "populate";
    }

    @Override
    public IBase getContextVariable() {
        return contextVariable;
    }

    public void setContextVariable(IBase value) {
        contextVariable = value;
    }

    @Override
    public IBase getResourceVariable() {
        return questionnaireResponseAdapter.get();
    }

    @Override
    public IBaseResource getQuestionnaire() {
        return questionnaireAdapter.get();
    }

    @Override
    public IQuestionnaireAdapter getQuestionnaireAdapter() {
        return questionnaireAdapter;
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
        return (IBaseParameters) getAdapterFactory()
                .createParameters(inputParameterResolver.getParameters())
                .copy();
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

    protected IQuestionnaireResponseAdapter createQuestionnaireResponse() {
        return getAdapterFactory()
                .createQuestionnaireResponse(getFhirContext()
                        .getResourceDefinition("QuestionnaireResponse")
                        .newInstance())
                .setId("%s-%s".formatted(questionnaireAdapter.getId().getIdPart(), subjectId.getIdPart()))
                .setQuestionnaire(questionnaireAdapter.getCanonical())
                .setSubject(subjectId)
                .setAuthored(new Date())
                .setStatus("in-progress");
    }

    public IQuestionnaireResponseAdapter getQuestionnaireResponseAdapter() {
        return questionnaireResponseAdapter;
    }

    public void addQuestionnaireResponseItems(List<IQuestionnaireResponseItemComponentAdapter> items) {
        questionnaireResponseAdapter.addItems(items);
    }
}
