package org.opencds.cqf.fhir.cr.questionnaire.populate;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.utility.Constants;

public class PopulateRequest implements IQuestionnaireRequest {
    private final String operationName;
    private final IBaseResource questionnaire;
    private final IIdType subjectId;
    private final IBaseParameters parameters;
    private final IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;
    private final boolean useServerData;
    private final IInputParameterResolver inputParameterResolver;
    private IBaseOperationOutcome operationOutcome;

    public PopulateRequest(
            String operationName,
            IBaseResource questionnaire,
            IIdType subjectId,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        this.operationName = operationName;
        this.questionnaire = questionnaire;
        this.subjectId = subjectId;
        this.parameters = parameters;
        this.data = data;
        this.useServerData = useServerData;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.fhirVersion = questionnaire.getStructureFhirVersionEnum();
        this.defaultLibraryUrl = resolveDefaultLibraryUrl();
        inputParameterResolver = IInputParameterResolver.createResolver(
                libraryEngine.getRepository(),
                this.subjectId,
                null,
                null,
                this.parameters,
                this.useServerData,
                this.data);
    }

    @Override
    public String getOperationName() {
        return operationName;
    }

    @Override
    public IBaseResource getQuestionnaire() {
        return questionnaire;
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

    @SuppressWarnings("unchecked")
    protected final String resolveDefaultLibraryUrl() {
        var libraryExt = getExtensions(questionnaire).stream()
                .filter(e -> e.getUrl()
                        .equals(fhirVersion == FhirVersionEnum.DSTU3 ? Constants.CQIF_LIBRARY : Constants.CQF_LIBRARY))
                .findFirst()
                .orElse(null);
        return libraryExt == null
                ? null
                : fhirVersion == FhirVersionEnum.DSTU3
                        ? ((Reference) libraryExt.getValue()).getReference()
                        : ((IPrimitiveType<String>) libraryExt.getValue()).getValue();
    }
}
