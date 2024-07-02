package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.Attachment;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.DataRequirement;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.UsageContext;
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
        final String referenceSource =
                hasVersion() ? getUrl() + "|" + getLibrary().getVersion() : getUrl();

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
    public <T extends ICompositeType> LibraryAdapter setDataRequirements(List<T> dataRequirements) {
        getLibrary()
                .setDataRequirement(dataRequirements.stream()
                        .map(dr -> (DataRequirement) dr)
                        .collect(Collectors.toList()));
        return this;
    }

    @Override
    public List<UsageContext> getUseContext() {
        return getLibrary().getUseContext();
    }
}
