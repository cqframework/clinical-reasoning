package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.cql.evaluator.fhir.util.DependencyInfo;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.visitor.r4.r4KnowledgeArtifactVisitor;

public class LibraryAdapter extends ResourceAdapter implements r4LibraryAdapter {

    private Library library;

    public LibraryAdapter(IBaseResource library) {
        super(library);
        if (!library.fhirType().equals("Library")) {
            throw new IllegalArgumentException("resource passed as library argument is not a Library resource");
        }
        this.library = (Library) library;
    }
    public LibraryAdapter(Library library) {
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
    @Override
    public List<DependencyInfo> getDependencies() {
        List<DependencyInfo> retval = new ArrayList<DependencyInfo>();
        final String source = this.getUrl();
        this.getRelatedArtifactsOfType(RelatedArtifactType.DEPENDSON).stream()
            .filter(ra -> ra.hasResource())
            .forEach(ra -> retval.add(new DependencyInfo(source, ra.getResource(), ra.getExtension())));
        this.get().getDataRequirement().stream()
            .forEach(dr -> {
                dr.getProfile().stream()
                    .filter(profile -> profile.hasValue())
                    .forEach(profile -> retval.add(new DependencyInfo(source, profile.getValue(), profile.getExtension())));
                dr.getCodeFilter().stream()
                    .filter(cf -> cf.hasValueSet())
                    .forEach(cf -> retval.add(new DependencyInfo(source, cf.getValueSet(), cf.getExtension())));
            });
        return retval;
    }

    public void setRelatedArtifact(List<RelatedArtifact> relatedArtifacts) {
        this.getLibrary().setRelatedArtifact(relatedArtifacts);
    }

    public IBase accept(r4KnowledgeArtifactVisitor visitor, Repository theRepository, Parameters theParameters) {
    return visitor.visit(this, theRepository, theParameters);
  }
  public Date getApprovalDate() {
    return this.getLibrary().getApprovalDate();
  }
  @Override
  public Period getEffectivePeriod(){
    return this.getLibrary().getEffectivePeriod();
  }
  @Override
  public void setEffectivePeriod(Period effectivePeriod) {
    this.getLibrary().setEffectivePeriod(effectivePeriod);
  }
  @Override
  public List<RelatedArtifact> getRelatedArtifact() {
    return this.getLibrary().getRelatedArtifact();
  }
}
