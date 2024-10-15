package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Attachment;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.ParameterDefinition;
import org.hl7.fhir.r5.model.Parameters;
import org.hl7.fhir.r5.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.UriType;
import org.hl7.fhir.r5.model.UsageContext;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class LibraryAdapter extends KnowledgeArtifactAdapter
        implements org.opencds.cqf.fhir.utility.adapter.LibraryAdapter {
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
        return getLibrary().getContent().stream().collect(Collectors.toList());
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
        List<IDependencyInfo> references = new ArrayList<IDependencyInfo>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        // relatedArtifact[].resource
        getRelatedArtifact().stream()
                .map(ra -> (RelatedArtifact) ra)
                .filter(ra -> ra.hasResource())
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .forEach(ra -> references.add(ra));
        getLibrary().getDataRequirement().stream().forEach(dr -> {
            dr.getProfile().stream()
                    .filter(profile -> profile.hasValue())
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource,
                            profile.getValue(),
                            profile.getExtension(),
                            (reference) -> profile.setValue(reference))));
            dr.getCodeFilter().stream()
                    .filter(cf -> cf.hasValueSet())
                    .forEach(cf -> references.add(new DependencyInfo(
                            referenceSource,
                            cf.getValueSet(),
                            cf.getExtension(),
                            (reference) -> cf.setValueSet(reference))));
        });
        return references;
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

    @SuppressWarnings("unchecked")
    @Override
    public List<DataRequirement> getDataRequirement() {
        return getLibrary().getDataRequirement();
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

    @SuppressWarnings("unchecked")
    @Override
    public List<UsageContext> getUseContext() {
        return getLibrary().getUseContext();
    }

    @Override
    public Optional<IBaseParameters> getExpansionParameters() {
        return getLibrary().getExtension().stream()
                .filter(ext -> ext.getUrl().equals(Constants.CQF_EXPANSION_PARAMETERS))
                .findAny()
                .map(ext -> ((Reference) ext.getValue()).getReference())
                .map(ref -> {
                    if (getLibrary().hasContained()) {
                        return getLibrary().getContained().stream()
                                .filter(containedResource ->
                                        containedResource.getId().equals(ref))
                                .findFirst()
                                .map(r -> (IBaseParameters) r)
                                .orElse(null);
                    }
                    return null;
                });
    }

    @Override
    public void setExpansionParameters(
            List<String> systemVersionExpansionParameters, List<String> canonicalVersionExpansionParameters) {
        var newParameters = new ArrayList<ParametersParameterComponent>();
        if (systemVersionExpansionParameters != null && !systemVersionExpansionParameters.isEmpty()) {
            for (String parameter : systemVersionExpansionParameters) {
                var param = new ParametersParameterComponent();
                param.setName(Constants.SYSTEM_VERSION);
                param.setValue(new UriType(parameter));
                newParameters.add(param);
            }
        }
        if (canonicalVersionExpansionParameters != null && !canonicalVersionExpansionParameters.isEmpty()) {
            for (String parameter : canonicalVersionExpansionParameters) {
                var param = new ParametersParameterComponent();
                param.setName(Constants.CANONICAL_VERSION);
                param.setValue(new UriType(parameter));
                newParameters.add(param);
            }
        }
        var existingExpansionParameters = getExpansionParameters();
        if (existingExpansionParameters.isPresent()) {
            ((Parameters) existingExpansionParameters.get()).setParameter(newParameters);
        } else {
            var id = "#exp-params";
            var newExpansionParameters = new Parameters();
            newExpansionParameters.setParameter(newParameters);
            newExpansionParameters.setId(id);
            getLibrary().addContained(newExpansionParameters);
            var expansionParamsExt = getLibrary().addExtension();
            expansionParamsExt.setUrl(Constants.CQF_EXPANSION_PARAMETERS);
            expansionParamsExt.setValue(new Reference(id));
        }
    }
}
