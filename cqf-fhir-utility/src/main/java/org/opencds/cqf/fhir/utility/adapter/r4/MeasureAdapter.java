package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.KnowledgeArtifactAdapter;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public class MeasureAdapter extends ResourceAdapter implements KnowledgeArtifactAdapter {

    private Measure measure;

    public MeasureAdapter(IDomainResource measure) {
        super(measure);

        if (!(measure instanceof Measure)) {
            throw new IllegalArgumentException("resource passed as measure argument is not a Measure resource");
        }

        this.measure = (Measure) measure;
    }

    public MeasureAdapter(Measure measure) {
        super(measure);
        this.measure = measure;
    }

    @Override
    public IBase accept(KnowledgeArtifactVisitor visitor, Repository repository, IBaseParameters operationParameters) {
        return visitor.visit(this, repository, operationParameters);
    }

    protected Measure getMeasure() {
        return this.measure;
    }

    @Override
    public Measure get() {
        return getMeasure();
    }

    @Override
    public Measure copy() {
        return get().copy();
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
                var edrReference = ((Reference) edrExtension.getValue()).getReference();
                for (var c : getMeasure().getContained()) {
                    if (c.hasId() && c.getId().equals(edrReference) && c instanceof Library) {
                        effectiveDataRequirements = (Library) c;
                        effectiveDataRequirementsAdapter = new LibraryAdapter(effectiveDataRequirements);
                    }
                }
            }
            checkedEffectiveDataRequirements = true;
        }
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        // If an effectiveDataRequirements library is present, use it exclusively
        findEffectiveDataRequirements();
        if (effectiveDataRequirements != null) {
            references.addAll(effectiveDataRequirementsAdapter.getDependencies());
            return references;
        }

        // Otherwise, fall back to the relatedArtifact and library

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
        references.addAll(getRelatedArtifact().stream()
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .collect(Collectors.toList()));

        // library[]
        for (var library : getMeasure().getLibrary()) {
            DependencyInfo dependency = new DependencyInfo(
                    referenceSource,
                    library.getValue(),
                    library.getExtension(),
                    (reference) -> library.setValue(reference));
            references.add(dependency);
        }

        for (final var group : getMeasure().getGroup()) {
            for (final var population : group.getPopulation()) {
                // group[].population[].criteria.reference
                if (population.getCriteria().hasReference()) {
                    final var dependency = new DependencyInfo(
                            referenceSource,
                            population.getCriteria().getReference(),
                            population.getCriteria().getExtension(),
                            (reference) -> population.getCriteria().setReference(reference));
                    references.add(dependency);
                }
            }
            for (final var stratifier : group.getStratifier()) {
                // group[].stratifier[].criteria.reference
                if (stratifier.getCriteria().hasReference()) {
                    final var dependency = new DependencyInfo(
                            referenceSource,
                            stratifier.getCriteria().getReference(),
                            stratifier.getCriteria().getExtension(),
                            (reference) -> stratifier.getCriteria().setReference(reference));
                    references.add(dependency);
                }
                for (final var component : stratifier.getComponent()) {
                    // group[].stratifier[].component[].criteria.reference
                    if (component.getCriteria().hasReference()) {
                        final var stratifierComponentDep = new DependencyInfo(
                                referenceSource,
                                component.getCriteria().getReference(),
                                component.getCriteria().getExtension(),
                                (reference) -> component.getCriteria().setReference(reference));
                        references.add(stratifierComponentDep);
                    }
                }
            }
        }

        for (final var supplement : getMeasure().getSupplementalData()) {
            // supplementalData[].criteria.reference
            if (supplement.getCriteria().hasReference()) {
                final var dependency = new DependencyInfo(
                        referenceSource,
                        supplement.getCriteria().getReference(),
                        supplement.getCriteria().getExtension(),
                        (reference) -> supplement.getCriteria().setReference(reference));
                references.add(dependency);
            }
        }

        // extension[cqfm-effectiveDataRequirements]
        // extension[crmi-effectiveDataRequirements]
        get().getExtension().stream()
                .filter(e -> CANONICAL_EXTENSIONS.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((CanonicalType) referenceExt.getValue()).getValue(),
                        referenceExt.getExtension(),
                        (reference) -> referenceExt.setValue(new CanonicalType(reference)))));

        // extension[cqfm-inputParameters][]
        // extension[cqfm-expansionParameters][]
        // extension[cqfm-cqlOptions]
        get().getExtension().stream()
                .filter(e -> REFERENCE_EXTENSIONS.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((Reference) referenceExt.getValue()).getReference(),
                        referenceExt.getExtension(),
                        (reference) -> referenceExt.setValue(new Reference(reference)))));

        // extension[cqfm-component][].resource
        get().getExtensionsByUrl(Constants.CQFM_COMPONENT).forEach(ext -> {
            final var ref = (RelatedArtifact) ext.getValue();
            if (ref.hasResource()) {
                final var dep = new DependencyInfo(
                        referenceSource,
                        ref.getResource(),
                        ref.getExtension(),
                        (reference) -> ref.setResource(reference));
                references.add(dep);
            }
        });

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
