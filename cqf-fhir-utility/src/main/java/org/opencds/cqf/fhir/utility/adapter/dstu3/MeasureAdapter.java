package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Library;
import org.hl7.fhir.dstu3.model.Measure;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class MeasureAdapter extends KnowledgeArtifactAdapter
        implements org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter {

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

    private String getEdrReferenceString(Extension edrExtension) {
        return edrExtension.getUrl().contains("cqfm")
                ? ((Reference) edrExtension.getValue()).getReference()
                : ((UriType) edrExtension.getValue()).getValue();
    }

    private Consumer<String> getEdrReferenceConsumer(Extension edrExtension) {
        return edrExtension.getUrl().contains("cqfm")
                ? reference -> edrExtension.setValue(new Reference(reference))
                : reference -> edrExtension.setValue(new UriType(reference));
    }

    private void findEffectiveDataRequirements() {
        if (!checkedEffectiveDataRequirements) {
            List<Extension> edrExtensions = this.getMeasure().getExtension().stream()
                    .filter(ext -> ext.getUrl().endsWith("-effectiveDataRequirements"))
                    .filter(Extension::hasValue)
                    .collect(Collectors.toList());

            var edrExtension = edrExtensions.size() == 1 ? edrExtensions.get(0) : null;
            // cqfm-effectiveDataRequirements is a Reference, crmi-effectiveDataRequirements is a canonical
            var maybeEdrReference = Optional.ofNullable(edrExtension).map(this::getEdrReferenceString);
            if (maybeEdrReference.isPresent()) {
                var edrReference = maybeEdrReference.get();
                for (var c : getMeasure().getContained()) {
                    if (c.hasId()
                            && (edrReference.equals(c.getId()) || edrReference.equals("#" + c.getId()))
                            && c instanceof Library) {
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
                    referenceSource, library.getReference(), library.getExtension(), library::setReference);
            references.add(dependency);
        }

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
            if (ref.hasResource() && ref.getResource().hasReference()) {
                final var dep = new DependencyInfo(
                        referenceSource,
                        ref.getResource().getReference(),
                        ref.getExtension(),
                        reference -> ref.getResource().setReference(reference));
                references.add(dep);
            }
        });

        return references;
    }
}
