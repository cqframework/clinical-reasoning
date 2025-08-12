package org.opencds.cqf.fhir.utility.adapter.r5;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.model.Library;
import org.hl7.fhir.r5.model.Measure;
import org.hl7.fhir.r5.model.PrimitiveType;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.UriType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter;

public class MeasureAdapter extends KnowledgeArtifactAdapter implements IMeasureAdapter {

    public MeasureAdapter(IDomainResource measure) {
        super(measure);
        if (!(measure instanceof Measure)) {
            // This is NOT due to a bad request/user error.  It's a system error.
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

    @Override
    public List<String> getLibrary() {
        return getMeasure().getLibrary().stream()
                .map(PrimitiveType::getValueAsString)
                .toList();
    }

    private String getEdrReferenceString(Extension edrExtension) {
        return edrExtension.getUrl().contains("cqfm")
                ? ((Reference) edrExtension.getValue()).getReference()
                : ((UriType) edrExtension.getValue()).getValue();
    }

    private Consumer<String> getEdrReferenceConsumer(Extension edrExtension) {
        return edrExtension.getUrl().contains("cqfm")
                ? reference -> edrExtension.setValue(new Reference(reference))
                : reference -> edrExtension.setValue(new CanonicalType(reference));
    }

    private void findEffectiveDataRequirements() {
        if (!checkedEffectiveDataRequirements) {
            var edrExtensions = this.getMeasure().getExtension().stream()
                    .filter(ext -> ext.getUrl().endsWith("-effectiveDataRequirements"))
                    .filter(Extension::hasValue)
                    .collect(Collectors.toList());

            var edrExtension = edrExtensions.size() == 1 ? edrExtensions.get(0) : null;
            // cqfm-effectiveDataRequirements is a Reference, crmi-effectiveDataRequirements is a canonical
            var maybeEdrReference = Optional.ofNullable(edrExtension).map(this::getEdrReferenceString);
            if (edrExtension != null) {
                var edrReference = maybeEdrReference.get();
                for (var c : getMeasure().getContained()) {
                    if (c.hasId()
                            && (edrReference.equals(c.getId()) || edrReference.equals("#" + c.getId()))
                            && c instanceof Library library) {
                        effectiveDataRequirements = library;
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
        getRelatedArtifactsOfType(DEPENDSON).stream()
                .filter(RelatedArtifact::hasResource)
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .forEach(references::add);

        // library[]
        for (var library : getMeasure().getLibrary()) {
            DependencyInfo dependency =
                    new DependencyInfo(referenceSource, library.getValue(), library.getExtension(), library::setValue);
            references.add(dependency);
        }

        for (final var group : getMeasure().getGroup()) {
            // group[].population[].criteria.reference
            group.getPopulation().stream()
                    .filter(p -> p.getCriteria().hasReference())
                    .forEach(p -> references.add(new DependencyInfo(
                            referenceSource,
                            p.getCriteria().getReference(),
                            p.getCriteria().getExtension(),
                            reference -> p.getCriteria().setReference(reference))));
            for (final var stratifier : group.getStratifier()) {
                // group[].stratifier[].criteria.reference
                if (stratifier.getCriteria().hasReference()) {
                    references.add(new DependencyInfo(
                            referenceSource,
                            stratifier.getCriteria().getReference(),
                            stratifier.getCriteria().getExtension(),
                            reference -> stratifier.getCriteria().setReference(reference)));
                }
                // group[].stratifier[].component[].criteria.reference
                stratifier.getComponent().stream()
                        .filter(c -> c.getCriteria().hasReference())
                        .forEach(component -> references.add(new DependencyInfo(
                                referenceSource,
                                component.getCriteria().getReference(),
                                component.getCriteria().getExtension(),
                                reference -> component.getCriteria().setReference(reference))));
            }
        }

        // supplementalData[].criteria.reference
        getMeasure().getSupplementalData().stream()
                .filter(s -> s.getCriteria().hasReference())
                .forEach(supplement -> references.add(new DependencyInfo(
                        referenceSource,
                        supplement.getCriteria().getReference(),
                        supplement.getCriteria().getExtension(),
                        reference -> supplement.getCriteria().setReference(reference))));

        // extension[cqfm-effectiveDataRequirements]
        // extension[crmi-effectiveDataRequirements]
        get().getExtension().stream()
                .filter(e -> CANONICAL_EXTENSIONS.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        getEdrReferenceString(referenceExt),
                        referenceExt.getExtension(),
                        getEdrReferenceConsumer(referenceExt))));

        // extension[cqfm-inputParameters][]
        // extension[cqfm-expansionParameters][]
        // extension[cqfm-cqlOptions]
        get().getExtension().stream()
                .filter(e -> REFERENCE_EXTENSIONS.contains(e.getUrl()))
                .forEach(referenceExt -> references.add(new DependencyInfo(
                        referenceSource,
                        ((Reference) referenceExt.getValue()).getReference(),
                        referenceExt.getExtension(),
                        reference -> referenceExt.setValue(new Reference(reference)))));

        // extension[cqfm-component][].resource
        get().getExtensionsByUrl(Constants.CQFM_COMPONENT).forEach(ext -> {
            final var ref = (RelatedArtifact) ext.getValue();
            if (ref.hasResource()) {
                final var dep =
                        new DependencyInfo(referenceSource, ref.getResource(), ref.getExtension(), ref::setResource);
                references.add(dep);
            }
        });

        return references;
    }
}
