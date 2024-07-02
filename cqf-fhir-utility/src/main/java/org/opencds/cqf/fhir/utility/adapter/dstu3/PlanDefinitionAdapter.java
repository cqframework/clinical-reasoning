package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

class PlanDefinitionAdapter extends KnowledgeArtifactAdapter {
    public PlanDefinitionAdapter(IDomainResource planDefinition) {
        super(planDefinition);
        if (!(planDefinition instanceof PlanDefinition)) {
            throw new IllegalArgumentException(
                    "resource passed as planDefinition argument is not a PlanDefinition resource");
        }
    }

    public PlanDefinitionAdapter(PlanDefinition planDefinition) {
        super(planDefinition);
    }

    protected PlanDefinition getPlanDefinition() {
        return (PlanDefinition) resource;
    }

    @Override
    public PlanDefinition get() {
        return getPlanDefinition();
    }

    @Override
    public PlanDefinition copy() {
        return get().copy();
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = hasVersion()
                ? getPlanDefinition().getUrl() + "|" + getPlanDefinition().getVersion()
                : getPlanDefinition().getUrl();
        /*
         https://build.fhir.org/ig/HL7/crmi-ig/distribution.html#package-and-data-requirements
         relatedArtifact[].resource
         library[]
         action[]..trigger[].dataRequirement[].profile[]
         action[]..trigger[].dataRequirement[].codeFilter[].valueSet
         action[]..condition[].expression.reference
         action[]..input[].profile[]
         action[]..input[].codeFilter[].valueSet
         action[]..output[].profile[]
         action[]..output[].codeFilter[].valueSet
         action[]..definitionCanonical
         action[]..dynamicValue[].expression.reference
         extension[cpg-partOf]
        */

        // relatedArtifact[].resource
        references.addAll(getRelatedArtifact().stream()
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .collect(Collectors.toList()));

        // library[]
        List<Reference> libraries = getPlanDefinition().getLibrary();
        for (Reference ref : libraries) {
            // TODO: Account for reference.identifier?
            DependencyInfo dependency = new DependencyInfo(
                    referenceSource,
                    ref.getReference(),
                    ref.getExtension(),
                    (reference) -> ref.setReference(reference));
            references.add(dependency);
        }
        // action[]
        getPlanDefinition().getAction().forEach(action -> getDependenciesOfAction(action, references, referenceSource));
        getPlanDefinition().getExtension().stream()
                .filter(ext -> ext.getUrl().contains("cpg-partOf"))
                .filter(ext -> ext.hasValue())
                .findAny()
                .ifPresent(ext -> {
                    references.add(new DependencyInfo(
                            referenceSource,
                            ((UriType) ext.getValue()).getValue(),
                            ext.getExtension(),
                            (reference) -> ((UriType) ext.getValue()).setValue(reference)));
                });
        // TODO: Ideally use $data-requirements code

        return references;
    }

    private void getDependenciesOfAction(
            PlanDefinition.PlanDefinitionActionComponent action,
            List<IDependencyInfo> references,
            String referenceSource) {
        action.getTriggerDefinition().stream().map(t -> t.getEventData()).forEach(eventData -> {
            // trigger[].dataRequirement[].profile[]
            eventData.getProfile().stream().filter(UriType::hasValue).forEach(profile -> {
                references.add(new DependencyInfo(
                        referenceSource,
                        profile.getValue(),
                        profile.getExtension(),
                        (reference) -> profile.setValue(reference)));
            });
            // trigger[].dataRequirement[].codeFilter[].valueSet
            eventData.getCodeFilter().stream().filter(cf -> cf.hasValueSet()).forEach(cf -> {
                references.add(dependencyFromDataRequirementCodeFilter(cf));
            });
        });
        Stream.concat(action.getInput().stream(), action.getOutput().stream()).forEach(inputOrOutput -> {
            // ..input[].profile[]
            // ..output[].profile[]
            inputOrOutput.getProfile().stream().filter(UriType::hasValue).forEach(profile -> {
                references.add(new DependencyInfo(
                        referenceSource,
                        profile.getValue(),
                        profile.getExtension(),
                        (reference) -> profile.setValue(reference)));
            });
            // input[].codeFilter[].valueSet
            // output[].codeFilter[].valueSet
            inputOrOutput.getCodeFilter().stream()
                    .filter(cf -> cf.hasValueSet())
                    .forEach(cf -> {
                        references.add(dependencyFromDataRequirementCodeFilter(cf));
                    });
        });
        // action..definition
        var definition = action.getDefinition();
        if (definition != null && definition.hasReference()) {
            references.add(new DependencyInfo(
                    referenceSource,
                    definition.getReference(),
                    definition.getExtension(),
                    (reference) -> definition.setReference(reference)));
        }
        action.getAction().forEach(nestedAction -> getDependenciesOfAction(nestedAction, references, referenceSource));
    }

    private DependencyInfo dependencyFromDataRequirementCodeFilter(DataRequirementCodeFilterComponent cf) {
        var vs = cf.getValueSet();
        if (vs instanceof StringType) {
            return new DependencyInfo(
                    getPlanDefinition().getUrl(),
                    ((StringType) vs).getValue(),
                    vs.getExtension(),
                    (reference) -> ((StringType) vs).setValue(reference));
        } else if (vs instanceof Reference) {
            return new DependencyInfo(
                    getPlanDefinition().getUrl(),
                    ((Reference) vs).getReference(),
                    vs.getExtension(),
                    (reference) -> ((Reference) vs).setReference(reference));
        }
        return null;
    }
}
