package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class MeasureAdapter extends KnowledgeArtifactAdapter {

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
                    if (c.hasId() && String.format("#%s", c.getId()).equals(edrReference) && c instanceof Library) {
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

        // If an effectiveDataRequirements library is present, use it exclusively
        findEffectiveDataRequirements();
        if (effectiveDataRequirements != null) {
            return effectiveDataRequirementsAdapter.getDependencies();
        }

        // Otherwise, fall back to the relatedArtifact and library
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = this.getMeasure().hasVersion()
                ? this.getMeasure().getUrl() + "|" + this.getMeasure().getVersion()
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
}
