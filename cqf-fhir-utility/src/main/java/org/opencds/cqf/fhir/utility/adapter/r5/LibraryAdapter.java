package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r5.model.Attachment;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

class LibraryAdapter extends ResourceAdapter implements r5LibraryAdapter {

    private Library library;

    public LibraryAdapter(IBaseResource library) {
        super(library);

        if (!library.fhirType().equals("Library")) {
            throw new IllegalArgumentException("resource passed as library argument is not a Library resource");
        }

        this.library = (Library) library;
    }

    protected Library getLibrary() {
        return this.library;
    }

    @Override
    public Library get() {
        return this.library;
    }

    @Override
    public IIdType getId() {
        return this.getLibrary().getIdElement();
    }

    @Override
    public void setId(IIdType id) {
        this.getLibrary().setId(id);
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
    public <T extends ICompositeType> void setContent(List<T> attachments) {
        List<Attachment> castAttachments =
                attachments.stream().map(x -> (Attachment) x).collect(Collectors.toList());
        this.getLibrary().setContent(castAttachments);
    }

    @Override
    public Attachment addContent() {
        return this.getLibrary().addContent();
    }

    public List<DependencyInfo> getDependencies() {
        return new ArrayList<DependencyInfo>();
    }
    public List<DependencyInfo> getComponents() {
        return new ArrayList<DependencyInfo>();
    }
    public IBase accept(KnowledgeArtifactVisitor visitor, Repository theRepository, IBaseParameters theParameters) {
    return visitor.visit(this, theRepository, theParameters);
  }
  public Date getApprovalDate() {
    return this.library.getApprovalDate();
  }
  public Period getEffectivePeriod() {
    return this.getLibrary().getEffectivePeriod();
  }
  public List<RelatedArtifact> getRelatedArtifact() {
    return this.getLibrary().getRelatedArtifact();
  }
}
