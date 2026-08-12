package org.opencds.cqf.fhir.cr.questionnaire.generate;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IQuestionnaireRequest;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;

@SuppressWarnings("UnstableApiUsage")
public class GenerateRequest implements IQuestionnaireRequest {
    private final List<IStructureDefinitionAdapter> profiles;
    private final List<String> addedProfiles;
    private IStructureDefinitionAdapter profileAdapter;
    private final boolean supportedOnly;
    private final boolean minimalOnly;
    private final LibraryEngine libraryEngine;
    private final FhirVersionEnum fhirVersion;
    private Map<String, String> referencedLibraries;
    private IQuestionnaireAdapter questionnaireAdapter;
    private List<IElementDefinitionAdapter> differentialElements;
    private List<IElementDefinitionAdapter> snapshotElements;

    public GenerateRequest(
            List<IBaseResource> profiles, boolean supportedOnly, boolean minimalOnly, LibraryEngine libraryEngine) {
        checkNotNull(profiles, "expected non-null value for profiles");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        if (profiles.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty list for profiles");
        }
        fhirVersion = libraryEngine.getRepository().fhirContext().getVersion().getVersion();
        this.profiles = profiles.stream()
                .map(p -> (IStructureDefinitionAdapter)
                        getAdapterFactory().createKnowledgeArtifactAdapter((IDomainResource) p))
                .toList();
        addedProfiles = new ArrayList<>();
        // setNextProfileAdapter();
        this.supportedOnly = supportedOnly;
        this.minimalOnly = minimalOnly;
        this.libraryEngine = libraryEngine;
        referencedLibraries = new HashMap<>();
    }

    public IBaseResource getProfile() {
        return profileAdapter.get();
    }

    public List<IStructureDefinitionAdapter> getProfiles() {
        return profiles;
    }

    public IStructureDefinitionAdapter getProfileAdapter() {
        return profileAdapter;
    }

    public boolean setNextProfileAdapter() {
        var profile = profiles.stream()
                .filter(p -> !addedProfiles.contains(p.getId()))
                .findFirst()
                .orElse(null);
        if (profile == null) {
            return false;
        }
        profileAdapter = profile;
        addedProfiles.add(profile.getId());
        referencedLibraries.putAll(profileAdapter.getReferencedLibraries());
        return true;
    }

    public IQuestionnaireAdapter getQuestionnaireAdapter() {
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
        if (questionnaire != null) {
            questionnaireAdapter = getAdapterFactory().createQuestionnaire(questionnaire);
        }
        return this;
    }

    public boolean getSupportedOnly() {
        return supportedOnly;
    }

    public boolean getMinimalOnly() {
        return minimalOnly;
    }

    public GenerateRequest setReferencedLibraries(Map<String, String> libraries) {
        referencedLibraries = libraries;
        return this;
    }

    @Override
    public String getOperationName() {
        return "questionnaire";
    }

    @Override
    public IBase getContextVariable() {
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
    public IBaseParameters getParameters() {
        throw new UnsupportedOperationException("Unimplemented method 'getParameters'");
    }

    @Override
    public LibraryEngine getLibraryEngine() {
        return libraryEngine;
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
    public IBaseResource getQuestionnaire() {
        return questionnaireAdapter == null ? null : questionnaireAdapter.get();
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

    public Set<String> getFHIRTypes() {
        var resourceTypes = libraryEngine.getRepository().fhirContext().getResourceTypes();
        resourceTypes.add("Resource");
        return resourceTypes;
    }
}
