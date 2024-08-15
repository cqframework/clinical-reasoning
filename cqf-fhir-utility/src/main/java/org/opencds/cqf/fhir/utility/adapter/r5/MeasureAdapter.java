package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

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
                        ((Reference) referenceExt.getValue()).getReference(),
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
}
