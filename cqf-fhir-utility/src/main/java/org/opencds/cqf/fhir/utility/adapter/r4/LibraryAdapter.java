package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.ParameterDefinition;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDataRequirementAdapter;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public class LibraryAdapter extends KnowledgeArtifactAdapter implements ILibraryAdapter {
    public LibraryAdapter(IDomainResource library) {
        super(library);
        if (!(library instanceof Library)) {
            throw new IllegalArgumentException("resource passed as library argument is not a Library resource");
        }
    }

    public LibraryAdapter(Library library) {
        super(library);
    }

    protected Library getLibrary() {
        return (Library) resource;
    }

    @Override
    public Library get() {
        return (Library) resource;
    }

    @Override
    public Library copy() {
        return get().copy();
    }

    @Override
    public boolean hasContent() {
        return getLibrary().hasContent();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Attachment> getContent() {
        return getLibrary().getContent().stream().toList();
    }

    @Override
    public void setContent(List<? extends ICompositeType> attachments) {
        List<Attachment> castAttachments =
                attachments.stream().map(x -> (Attachment) x).collect(Collectors.toList());
        getLibrary().setContent(castAttachments);
    }

    @Override
    public Attachment addContent() {
        return getLibrary().addContent();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        // relatedArtifact[].resource
        var relatedArtifacts = getRelatedArtifactsOfType(DEPENDSON);
        for (int i = 0; i < relatedArtifacts.size(); i++) {
            RelatedArtifact ra = relatedArtifacts.get(i);
            if (ra.hasResource()) {
                IDependencyInfo dep = DependencyInfo.convertRelatedArtifact(ra, referenceSource);
                dep.addFhirPath("relatedArtifact[" + i + "].resource");
                references.add(dep);
            }
        }

        var dataRequirements = getLibrary().getDataRequirement();
        for (int drIndex = 0; drIndex < dataRequirements.size(); drIndex++) {
            var dr = dataRequirements.get(drIndex);

            // dataRequirement[].profile[]
            var profiles = dr.getProfile();
            for (int profileIndex = 0; profileIndex < profiles.size(); profileIndex++) {
                var profile = profiles.get(profileIndex);
                if (profile.hasValue()) {
                    IDependencyInfo dep = new DependencyInfo(
                            referenceSource, profile.getValue(), profile.getExtension(), profile::setValue);
                    dep.addFhirPath("dataRequirement[" + drIndex + "].profile[" + profileIndex + "]");
                    references.add(dep);
                }
            }

            // dataRequirement[].codeFilter[].valueSet
            var codeFilters = dr.getCodeFilter();
            for (int cfIndex = 0; cfIndex < codeFilters.size(); cfIndex++) {
                var cf = codeFilters.get(cfIndex);
                if (cf.hasValueSet()) {
                    IDependencyInfo dep =
                            new DependencyInfo(referenceSource, cf.getValueSet(), cf.getExtension(), cf::setValueSet);
                    dep.addFhirPath("dataRequirement[" + drIndex + "].codeFilter[" + cfIndex + "].valueSet");
                    references.add(dep);
                }
            }
        }

        return references;
    }

    @Override
    public Map<String, String> getReferencedLibraries() {
        var map = new HashMap<String, String>();
        map.put(getName(), getCanonical());
        return map;
    }

    @Override
    public Map<String, ILibraryAdapter> retrieveReferencedLibraries(IRepository repository) {
        var map = new HashMap<String, ILibraryAdapter>();
        map.put(getName(), this);
        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getComponents() {
        return getRelatedArtifactsOfType("composed-of");
    }

    @Override
    public ICompositeType getType() {
        return getLibrary().getType();
    }

    @Override
    public LibraryAdapter setType(String type) {
        if (LIBRARY_TYPES.contains(type)) {
            getLibrary()
                    .setType(new CodeableConcept(new Coding("http://hl7.org/fhir/ValueSet/library-type", type, "")));
        } else {
            throw new UnprocessableEntityException("Invalid type: {}", type);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ParameterDefinition> getParameter() {
        return getLibrary().getParameter();
    }

    @Override
    public boolean hasDataRequirement() {
        return getLibrary().hasDataRequirement();
    }

    @Override
    public List<IDataRequirementAdapter> getDataRequirement() {
        return getLibrary().getDataRequirement().stream()
                .map(DataRequirementAdapter::new)
                .collect(Collectors.toList());
    }

    @Override
    public LibraryAdapter addDataRequirement(ICompositeType dataRequirement) {
        getLibrary().addDataRequirement((DataRequirement) dataRequirement);
        return this;
    }

    @Override
    public <T extends ICompositeType> LibraryAdapter setDataRequirement(List<T> dataRequirement) {
        getLibrary()
                .setDataRequirement(
                        dataRequirement.stream().map(dr -> (DataRequirement) dr).collect(Collectors.toList()));
        return this;
    }

    @Override
    public Optional<IBaseParameters> getExpansionParameters() {
        var expansionParameters = getLibrary().getExtension().stream()
                .filter(ext -> ext.getUrl().equals(Constants.CQF_EXPANSION_PARAMETERS))
                .findAny()
                .map(ext -> ((Reference) ext.getValue()).getReference())
                .map(ref -> {
                    if (getLibrary().hasContained()) {
                        return getLibrary().getContained().stream()
                                .filter(containedResource -> ref.equals("#" + containedResource.getId()))
                                .filter(IBaseParameters.class::isInstance)
                                .map(IBaseParameters.class::cast)
                                .findFirst()
                                .orElse(null);
                    }
                    return null;
                });

        if (expansionParameters.isPresent()) {
            return expansionParameters;
        } else {
            var id = "exp-params";
            var newExpansionParameters = new Parameters();
            newExpansionParameters.setId(id);
            getLibrary().addContained(newExpansionParameters);
            if (getLibrary().getExtensionByUrl(Constants.CQF_EXPANSION_PARAMETERS) == null) {
                var expansionParamsExt = getLibrary().addExtension();
                expansionParamsExt.setUrl(Constants.CQF_EXPANSION_PARAMETERS);
                expansionParamsExt.setValue(new Reference("#" + id));
            }
            setExpansionParameters(newExpansionParameters);
            return Optional.of(newExpansionParameters);
        }
    }

    @Override
    public void setExpansionParameters(IBaseParameters expansionParameters) {
        if (expansionParameters != null
                && !((Parameters) expansionParameters).getParameter().isEmpty()) {
            var newParameters = new ArrayList<ParametersParameterComponent>();

            for (ParametersParameterComponent parameter : ((Parameters) expansionParameters).getParameter()) {
                var param = new ParametersParameterComponent();
                param.setName(parameter.getName());
                param.setValue(parameter.getValue());
                newParameters.add(param);
            }

            var existingExpansionParameters = getExpansionParameters();
            existingExpansionParameters.ifPresent(parameters -> ((Parameters) parameters).setParameter(newParameters));
        }
    }
}
