package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;

public class GenerateRequest implements IQuestionnaireRequest {
    private final boolean supportedOnly;
    private final boolean requiredOnly;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final IBaseResource profile;
    private String defaultLibraryUrl;
    private IBaseResource questionnaire;
    private IQuestionnaireAdapter questionnaireAdapter;
    private IStructureDefinitionAdapter profileAdapter;
    private List<IElementDefinitionAdapter> differentialElements;
    private List<IElementDefinitionAdapter> snapshotElements;

    public GenerateRequest(
            IBaseResource profile,
            boolean supportedOnly,
            boolean requiredOnly,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(profile, "expected non-null value for profile");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        this.profile = profile;
        fhirVersion = this.profile.getStructureFhirVersionEnum();
        this.supportedOnly = supportedOnly;
        this.requiredOnly = requiredOnly;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        defaultLibraryUrl = resolveDefaultLibraryUrl();
    }

    public IBaseResource getProfile() {
        return profile;
    }

    public IStructureDefinitionAdapter getProfileAdapter() {
        if (profileAdapter == null) {
            profileAdapter = (IStructureDefinitionAdapter)
                    getAdapterFactory().createKnowledgeArtifactAdapter((IDomainResource) profile);
        }
        return profileAdapter;
    }

    public IQuestionnaireAdapter getQuestionnaireAdapter() {
        if (questionnaireAdapter == null && questionnaire != null) {
            questionnaireAdapter = (IQuestionnaireAdapter)
                    getAdapterFactory().createKnowledgeArtifactAdapter((IDomainResource) questionnaire);
        }
        return questionnaireAdapter;
    }

    public void setDifferentialElements(List<IElementDefinitionAdapter> elements) {
        differentialElements = elements;
    }

    public List<IElementDefinitionAdapter> getDifferentialElements() {
        return differentialElements;
    }

    public void setSnapshotElements(List<IElementDefinitionAdapter> elements) {
        snapshotElements = elements;
    }

    public List<IElementDefinitionAdapter> getSnapshotElements() {
        return snapshotElements;
    }

    public GenerateRequest setQuestionnaire(IBaseResource questionnaire) {
        this.questionnaire = questionnaire;
        return this;
    }

    public boolean getSupportedOnly() {
        return supportedOnly;
    }

    public boolean getRequiredOnly() {
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
    public IBase getContext() {
        return getProfile();
    }

    @Override
    public IIdType getSubjectId() {
        throw new UnsupportedOperationException("Unimplemented method 'getSubjectId'");
    }

    @Override
    public IBaseBundle getData() {
        throw new UnsupportedOperationException("Unimplemented method 'getData'");
    }

    @Override
    public boolean getUseServerData() {
        throw new UnsupportedOperationException("Unimplemented method 'getUseServerData'");
    }

    @Override
    public IBaseParameters getParameters() {
        throw new UnsupportedOperationException("Unimplemented method 'getParameters'");
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
        return libraryExt == null ? null : ((IPrimitiveType<String>) libraryExt.getValue()).getValue();
    }
}
