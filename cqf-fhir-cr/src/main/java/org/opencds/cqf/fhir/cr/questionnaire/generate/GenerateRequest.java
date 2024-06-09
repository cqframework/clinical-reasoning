package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.utility.Constants;

public class GenerateRequest implements IQuestionnaireRequest {
    private final boolean supportedOnly;
    private final boolean requiredOnly;
    private final IIdType subjectId;
    private final IBaseParameters parameters;
    private final boolean useServerData;
    private final IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final IBaseResource profile;
    private final String profileUrl;
    private String defaultLibraryUrl;
    private IBaseResource questionnaire;
    private List<? extends ICompositeType> differentialElements;
    private List<? extends ICompositeType> snapshotElements;

    public GenerateRequest(
            IBaseResource profile,
            boolean supportedOnly,
            boolean requiredOnly,
            IIdType subjectId,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        this.profile = profile;
        this.supportedOnly = supportedOnly;
        this.requiredOnly = requiredOnly;
        this.subjectId = subjectId;
        this.parameters = parameters;
        this.useServerData = useServerData;
        this.data = data;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        fhirVersion =
                this.libraryEngine.getRepository().fhirContext().getVersion().getVersion();
        defaultLibraryUrl = resolveDefaultLibraryUrl();
        profileUrl = resolvePathString(this.profile, "url");
    }

    public IBaseResource getProfile() {
        return profile;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public <E extends ICompositeType> void setDifferentialElements(List<E> elements) {
        differentialElements = elements;
    }

    @SuppressWarnings("unchecked")
    public <E extends ICompositeType> List<E> getDifferentialElements() {
        return (List<E>) differentialElements;
    }

    public <E extends ICompositeType> void setSnapshotElements(List<E> elements) {
        snapshotElements = elements;
    }

    @SuppressWarnings("unchecked")
    public <E extends ICompositeType> List<E> getSnapshotElements() {
        return (List<E>) snapshotElements;
    }

    public GenerateRequest setQuestionnaire(IBaseResource questionnaire) {
        this.questionnaire = questionnaire;
        return this;
    }

    public Boolean getSupportedOnly() {
        return supportedOnly;
    }

    public Boolean getRequiredOnly() {
        return requiredOnly;
    }

    public GenerateRequest setDefaultLibraryUrl(String url) {
        defaultLibraryUrl = url;
        return this;
    }

    @Override
    public String getOperationName() {
        return "questionnaire";
    }

    @Override
    public IIdType getSubjectId() {
        return subjectId;
    }

    @Override
    public IBaseParameters getParameters() {
        return parameters;
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
    public IBaseResource getQuestionnaire() {
        return questionnaire;
    }

    @Override
    public IBaseOperationOutcome getOperationOutcome() {
        // Errors during Questionnaire generation manifest as error items
        throw new UnsupportedOperationException("Unimplemented method 'getOperationOutcome'");
    }

    @Override
    public void setOperationOutcome(IBaseOperationOutcome operationOutcome) {
        // Errors during Questionnaire generation manifest as error items
        throw new UnsupportedOperationException("Unimplemented method 'setOperationOutcome'");
    }

    @SuppressWarnings("unchecked")
    protected final String resolveDefaultLibraryUrl() {
        var libraryExt = getExtensions(profile).stream()
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
