package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.visitor.KnowledgeArtifactVisitor;

public class MeasureAdapter extends KnowledgeArtifactAdapter
        implements org.opencds.cqf.fhir.utility.adapter.MeasureAdapter {

    public MeasureAdapter(IDomainResource measure) {
        super(measure);
        if (!(measure instanceof Measure)) {
            throw new IllegalArgumentException("resource passed as measure argument is not a Measure resource");
        }
    }

    public MeasureAdapter(Measure measure) {
        super(measure);
    }

    protected Measure getMeasure() {
        return (Measure) resource;
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
         group[].population[].criteria.reference - no path on dstu3
         group[].stratifier[].criteria.reference - no path on dstu3
         group[].stratifier[].component[].criteria.reference - no path on dstu3
         supplementalData[].criteria.reference - no path on dstu3
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
            final var dependency = new DependencyInfo(
                    referenceSource,
                    library.getReference(),
                    library.getExtension(),
                    (reference) -> library.setReference(reference));
            references.add(dependency);
        }

        // extension[cqfm-effectiveDataRequirements]
        // extension[crmi-effectiveDataRequirements]
        get().getExtension().stream()
                .filter(e -> CANONICAL_EXTENSIONS.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((Reference) referenceExt.getValue()).getReference(),
                        referenceExt.getExtension(),
                        (reference) -> referenceExt.setValue(new Reference(reference)))));

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
            if (ref.hasResource() && ref.getResource().hasReference()) {
                final var dep = new DependencyInfo(
                        referenceSource,
                        ref.getResource().getReference(),
                        ref.getExtension(),
                        (reference) -> ref.getResource().setReference(reference));
                references.add(dep);
            }
        });

        return references;
    }
}
