package org.opencds.cqf.fhir.cr.questionnaire.populate;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.utility.Constants;

public class PopulateRequest implements IQuestionnaireRequest {
    private final String operationName;
    private final IBaseResource questionnaire;
    private final IIdType subjectId;
    private final IBaseParameters parameters;
    private final IBaseBundle bundle;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;
    private IBaseOperationOutcome operationOutcome;

    // test constructor
    public PopulateRequest(FhirVersionEnum fhirVersion, String operationName) {
        this.operationName = operationName;
        this.questionnaire = null;
        this.subjectId = null;
        this.parameters = null;
        this.bundle = null;
        this.libraryEngine = null;
        this.modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.fhirVersion = fhirVersion;
        this.defaultLibraryUrl = null;
    }

    public PopulateRequest(
            String operationName,
            IBaseResource questionnaire,
            IIdType subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        this.operationName = operationName;
        this.questionnaire = questionnaire;
        this.subjectId = subjectId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.fhirVersion = questionnaire.getStructureFhirVersionEnum();
        this.defaultLibraryUrl = resolveDefaultLibraryUrl();
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
    public IBaseBundle getBundle() {
        return bundle;
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

    @SuppressWarnings("unchecked")
    protected final String resolveDefaultLibraryUrl() {
        var libraryExt = getExtensions(questionnaire).stream()
                .filter(e -> e.getUrl().equals(Constants.CQF_LIBRARY))
                .findFirst()
                .orElse(null);
        return libraryExt == null ? null : ((IPrimitiveType<String>) libraryExt.getValue()).getValue();
    }
}
