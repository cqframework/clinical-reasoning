package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.DateTimeType;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.Period;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MeasureAdapter extends ResourceAdapter implements KnowledgeArtifactAdapter {

    private Measure measure;

    public MeasureAdapter(IDomainResource measure) {
        super(measure);

        if (!(measure instanceof Measure)) {
            throw new IllegalArgumentException(
                "resource passed as measure argument is not a Measure resource");
        }

        this.measure = (Measure) measure;
    }

    public MeasureAdapter(Measure measure) {
        super(measure);
        this.measure = measure;
    }

    @Override
    public IBase accept(
        KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
    }

    protected Measure getMeasure() {
        return this.measure;
    }

    @Override
    public Measure get() {
        return this.measure;
    }

    @Override
    public Measure copy() {
        return this.get().copy();
    }

    @Override
    public IIdType getId() {
        return this.getMeasure().getIdElement();
    }

    @Override
    public void setId(IIdType id) {
        this.getMeasure().setId(id);
    }

    @Override
    public String getName() {
        return this.getMeasure().getName();
    }

    @Override
    public boolean hasTitle() {
        return this.getMeasure().hasTitle();
    }

    @Override
    public String getTitle() {
        return this.getMeasure().getTitle();
    }

    @Override
    public String getPurpose() {
        return this.getMeasure().getPurpose();
    }

    @Override
    public void setName(String name) {
        this.getMeasure().setName(name);
    }

    @Override
    public void setTitle(String title) {
        this.getMeasure().setTitle(title);
    }

    @Override
    public String getUrl() {
        return this.getMeasure().getUrl();
    }

    @Override
    public boolean hasUrl() {
        return this.getMeasure().hasUrl();
    }

    @Override
    public void setUrl(String url) {
        this.getMeasure().setUrl(url);
    }

    @Override
    public String getVersion() {
        return this.getMeasure().getVersion();
    }

    @Override
    public boolean hasVersion() {
        return this.getMeasure().hasVersion();
    }

    @Override
    public void setVersion(String version) {
        this.getMeasure().setVersion(version);
    }

    private boolean checkedEffectiveDataRequirements;
    private Library effectiveDataRequirements;
    private LibraryAdapter effectiveDataRequirementsAdapter;

    private void findEffectiveDataRequirements() {
        if (!checkedEffectiveDataRequirements) {
            var edrExtensions = this.getMeasure().getExtension().stream()
                .filter(ext -> ext.getUrl().endsWith("-effectiveDataRequirements"))
                .filter(ext -> ext.hasValue())
                .collect(Collectors.toList());

            var edrExtension = edrExtensions.size() == 1 ? edrExtensions.get(0) : null;
            if (edrExtension != null) {
                var edrReference = ((Reference)edrExtension.getValue()).getReference();
                for (var c : getMeasure().getContained()) {
                    if (c.hasId() && String.format("#%s", c.getId()).equals(edrReference) && c instanceof Library) {
                        effectiveDataRequirements = (Library)c;
                        effectiveDataRequirementsAdapter = new LibraryAdapter(effectiveDataRequirements);
                    }
                }
            }
            checkedEffectiveDataRequirements = true;
        }
    }

    @Override
    public List<IDependencyInfo> getDependencies() {

        // If an effectiveDataRequirements library is present, use it exclusively
        findEffectiveDataRequirements();
        if (effectiveDataRequirements != null) {
            return effectiveDataRequirementsAdapter.getDependencies();
        }

        // Otherwise, fall back to the relatedArtifact and library
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = this.getMeasure().hasVersion()
            ? this.getMeasure().getUrl() + "|"
            + this.getMeasure().getVersion()
            : this.getMeasure().getUrl();
        /*
         relatedArtifact[].resource
         library[]
         group[].population[].criteria.reference
         group[].stratifier[].criteria.reference
         group[].stratifier[].component[].criteria.reference
         supplementalData[].criteria.reference
         extension[cqfm-inputParameters][]
         extension[cqfm-expansionParameters][]
         extension[cqfm-effectiveDataRequirements]
         extension[cqfm-cqlOptions]
         extension[cqfm-component][].resource
         extension[crmi-effectiveDataRequirements]
        */

        // relatedArtifact[].resource
        references.addAll(this.getRelatedArtifact().stream()
            .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
            .collect(Collectors.toList()));

        // library[]
        List<CanonicalType> libraries = this.getMeasure().getLibrary();
        for (CanonicalType ct : libraries) {
            DependencyInfo dependency = new DependencyInfo(
                referenceSource, ct.getValue(), ct.getExtension(), (reference) -> ct.setValue(reference));
            references.add(dependency);
        }

        return references;
    }

    @Override
    public Date getApprovalDate() {
        return this.getMeasure().getApprovalDate();
    }

    @Override
    public Date getDate() {
        return this.getMeasure().getDate();
    }

    @Override
    public void setDate(Date date) {
        this.getMeasure().setDate(date);
    }

    @Override
    public void setDateElement(IPrimitiveType<Date> date) {
        if (date != null && !(date instanceof DateTimeType)) {
            throw new UnprocessableEntityException("Date must be " + DateTimeType.class.getName());
        }
        this.getMeasure().setDateElement((DateTimeType) date);
    }

    @Override
    public Period getEffectivePeriod() {
        return this.getMeasure().getEffectivePeriod();
    }

    @Override
    public void setApprovalDate(Date date) {
        this.getMeasure().setApprovalDate(date);
    }

    @Override
    public boolean hasRelatedArtifact() {
        return this.getMeasure().hasRelatedArtifact();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RelatedArtifact> getRelatedArtifact() {
        return this.getMeasure().getRelatedArtifact();
    }

    @Override
    public <T extends ICompositeType & IBaseHasExtensions> void setRelatedArtifact(List<T> relatedArtifacts) {
        this.getMeasure()
            .setRelatedArtifact(relatedArtifacts.stream()
                .map(ra -> (RelatedArtifact) ra)
                .collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
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
    public void setEffectivePeriod(ICompositeType effectivePeriod) {
        if (effectivePeriod != null && !(effectivePeriod instanceof Period)) {
            throw new UnprocessableEntityException("EffectivePeriod must be " + Period.class.getName());
        }
        this.getMeasure().setEffectivePeriod((Period) effectivePeriod);
    }

    @Override
    public void setStatus(String statusCodeString) {
        PublicationStatus status;
        try {
            status = PublicationStatus.fromCode(statusCodeString);
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid status code");
        }
        this.getMeasure().setStatus(status);
    }

    @Override
    public String getStatus() {
        return this.getMeasure().getStatus() == null
            ? null
            : this.getMeasure().getStatus().toCode();
    }

    @Override
    public boolean getExperimental() {
        return this.getMeasure().getExperimental();
    }

    @Override
    public void setExtension(List<IBaseExtension<?, ?>> extensions) {
        this.get().setExtension(extensions.stream().map(e -> (Extension) e).collect(Collectors.toList()));
    }
}