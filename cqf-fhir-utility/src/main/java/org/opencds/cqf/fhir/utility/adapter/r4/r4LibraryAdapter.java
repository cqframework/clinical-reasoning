package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.LibraryAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public class r4LibraryAdapter extends ResourceAdapter implements LibraryAdapter {

    private Library library;

    public r4LibraryAdapter(IBaseResource library) {
        super(library);
        if (!library.fhirType().equals("Library")) {
            throw new IllegalArgumentException("resource passed as library argument is not a Library resource");
        }
        this.library = (Library) library;
    }

    public r4LibraryAdapter(Library library) {
        super(library);
        this.library = library;
    }

    protected Library getLibrary() {
        return this.library;
    }

    @Override
    public Library get() {
        return this.library;
    }

    @Override
    public String getName() {
        return this.getLibrary().getName();
    }

    @Override
    public void setName(String name) {
        this.getLibrary().setName(name);
    }

    @Override
    public String getUrl() {
        return this.getLibrary().getUrl();
    }

    @Override
    public void setUrl(String url) {
        this.getLibrary().setUrl(url);
    }

    @Override
    public String getVersion() {
        return this.getLibrary().getVersion();
    }

    @Override
    public boolean hasVersion() {
        return this.getLibrary().hasVersion();
    }

    @Override
    public void setVersion(String version) {
        this.getLibrary().setVersion(version);
    }

    @Override
    public boolean hasContent() {
        return this.getLibrary().hasContent();
    }

    @Override
    public List<Attachment> getContent() {
        return this.getLibrary().getContent().stream().collect(Collectors.toList());
    }

    @Override
    public void setContent(List<? extends ICompositeType> attachments) {
        List<Attachment> castAttachments =
                attachments.stream().map(x -> (Attachment) x).collect(Collectors.toList());
        this.getLibrary().setContent(castAttachments);
    }

    @Override
    public Attachment addContent() {
        return this.getLibrary().addContent();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> retval = new ArrayList<IDependencyInfo>();
        final String source = this.getUrl();
        this.getRelatedArtifactsOfType("depends-on").stream()
                .filter(ra -> ra.hasResource())
                .forEach(ra -> retval.add(new DependencyInfo(source, ra.getResource(), ra.getExtension())));
        this.get().getDataRequirement().stream().forEach(dr -> {
            dr.getProfile().stream()
                    .filter(profile -> profile.hasValue())
                    .forEach(profile ->
                            retval.add(new DependencyInfo(source, profile.getValue(), profile.getExtension())));
            dr.getCodeFilter().stream()
                    .filter(cf -> cf.hasValueSet())
                    .forEach(cf -> retval.add(new DependencyInfo(source, cf.getValueSet(), cf.getExtension())));
        });
        return retval;
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts)
            throws ClassCastException {
        this.getLibrary()
                .setRelatedArtifact(relatedArtifacts.stream()
                        .map(ra -> (RelatedArtifact) ra)
                        .collect(Collectors.toList()));
    }

    @Override
    public IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
    }

    @Override
    public Date getApprovalDate() {
        return this.getLibrary().getApprovalDate();
    }

    @Override
    public Date getDate() {
        return this.getLibrary().getDate();
    }

    @Override
    public void setDate(Date date) {
        this.getLibrary().setDate(date);
    }

    @Override
    public void setDateElement(IPrimitiveType<Date> date) {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        this.getLibrary().setDateElement((DateTimeType) date);
    }

    @Override
    public Period getEffectivePeriod() {
        return this.getLibrary().getEffectivePeriod();
    }

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be org.hl7.fhir.r4.model.Period");
        }
        this.getLibrary().setEffectivePeriod((Period) effectivePeriod);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return this.getLibrary().getRelatedArtifact();
    }

    @Override
    public void setApprovalDate(Date approvalDate) {
        this.getLibrary().setApprovalDate(approvalDate);
    }

    @Override
    public List<RelatedArtifact> getRelatedArtifactsOfType(String codeString) {
        RelatedArtifactType type;
        try {
            type = RelatedArtifactType.fromCode(codeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid related artifact code");
        }
        return this.getRelatedArtifact().stream()
                .filter(ra -> ra.getType() == type)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getComponents() {
        return this.getRelatedArtifactsOfType("composed-of");
    }

    @Override
    public void setStatus(String statusCodeString) {
        PublicationStatus type;
        try {
            type = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        this.getLibrary().setStatus(type);
    }
}
