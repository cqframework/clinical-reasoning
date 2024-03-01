package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Attachment;
import org.hl7.fhir.dstu3.model.DataRequirement;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

class LibraryAdapter extends ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.LibraryAdapter {

    private Library library;

    public LibraryAdapter(IBaseResource library) {
        super(library);

        if (!library.fhirType().equals("Library")) {
            throw new IllegalArgumentException("resource passed as library argument is not a Library resource");
        }

        this.library = (Library) library;
    }

    public IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
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
    public boolean hasVersion() {
        return this.getLibrary().hasVersion();
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
        final String referenceSource =
                this.hasVersion() ? this.library.getUrl() + "|" + this.library.getVersion() : this.library.getUrl();
        List<IDependencyInfo> references = new ArrayList<>();

        // relatedArtifact[].resource
        references.addAll(this.getRelatedArtifact().stream()
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .collect(Collectors.toList()));

        // dataRequirement
        List<DataRequirement> dataRequirements = this.library.getDataRequirement();
        for (DataRequirement dr : dataRequirements) {
            // dataRequirement.profile[]
            List<UriType> profiles = dr.getProfile();
            for (UriType uri : profiles) {
                if (uri.hasValue()) {
                    DependencyInfo dependency = new DependencyInfo(referenceSource, uri.getValue(), dr.getExtension());
                    references.add(dependency);
                }
            }

            // dataRequirement.codeFilter[].valueset
            List<DataRequirement.DataRequirementCodeFilterComponent> codeFilters = dr.getCodeFilter();
            for (DataRequirement.DataRequirementCodeFilterComponent cf : codeFilters) {
                if (cf.hasValueSet()) {
                    DependencyInfo dependency = new DependencyInfo(
                            referenceSource, cf.getValueSetReference().getReference(), cf.getExtension());
                    references.add(dependency);
                }
            }
        }
        return references;
    }

    @Override
    public Date getApprovalDate() {
        return this.getLibrary().getApprovalDate();
    }

    @Override
    public void setApprovalDate(Date approvalDate) {
        this.getLibrary().setApprovalDate(approvalDate);
    }

    @Override
    public Date getDate() {
        return this.getLibrary().getDate();
    }

    @Override
    public void setDate(Date approvalDate) {
        this.getLibrary().setDate(approvalDate);
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
    public List<RelatedArtifact> getRelatedArtifact() {
        return this.getLibrary().getRelatedArtifact();
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

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts)
            throws ClassCastException {
        this.getLibrary()
                .setRelatedArtifact(relatedArtifacts.stream()
                        .map(ra -> (RelatedArtifact) ra)
                        .collect(Collectors.toList()));
    }

    @Override
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be org.hl7.fhir.r4.model.Period");
        }
        this.getLibrary().setEffectivePeriod((Period) effectivePeriod);
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
