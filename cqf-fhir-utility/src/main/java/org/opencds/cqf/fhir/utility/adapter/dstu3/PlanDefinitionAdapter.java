package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.TriggerDefinition;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;

class PlanDefinitionAdapter extends KnowledgeArtifactAdapter implements IPlanDefinitionAdapter {
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
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

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
        getRelatedArtifactsOfType(DEPENDSON).stream()
                .filter(RelatedArtifact::hasResource)
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .forEach(references::add);

        // library[]
        List<Reference> libraries = getPlanDefinition().getLibrary();
        for (Reference ref : libraries) {
            DependencyInfo dependency =
                    new DependencyInfo(referenceSource, ref.getReference(), ref.getExtension(), ref::setReference);
            references.add(dependency);
        }
        // action[]
        getPlanDefinition().getAction().forEach(action -> getDependenciesOfAction(action, references, referenceSource));
        getPlanDefinition().getExtension().stream()
                .filter(ext -> ext.getUrl().contains("cpg-partOf"))
                .filter(Extension::hasValue)
                .findAny()
                .ifPresent(ext -> {
                    final var reference = (UriType) ext.getValue();
                    references.add(new DependencyInfo(
                            referenceSource, reference.getValue(), ext.getExtension(), reference::setValue));
                });
        return references;
    }

    private void getDependenciesOfAction(
            PlanDefinition.PlanDefinitionActionComponent action,
            List<IDependencyInfo> references,
            String referenceSource) {
        action.getTriggerDefinition().stream()
                .map(TriggerDefinition::getEventData)
                .forEach(eventData -> {
                    // trigger[].dataRequirement[].profile[]
                    eventData.getProfile().stream()
                            .filter(UriType::hasValue)
                            .forEach(profile -> references.add(new DependencyInfo(
                                    referenceSource, profile.getValue(), profile.getExtension(), profile::setValue)));
                    // trigger[].dataRequirement[].codeFilter[].valueSet
                    eventData.getCodeFilter().stream()
                            .filter(DataRequirementCodeFilterComponent::hasValueSet)
                            .forEach(cf -> references.add(dependencyFromDataRequirementCodeFilter(cf)));
                });
        Stream.concat(action.getInput().stream(), action.getOutput().stream()).forEach(inputOrOutput -> {
            // ..input[].profile[]
            // ..output[].profile[]
            inputOrOutput.getProfile().stream()
                    .filter(UriType::hasValue)
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource, profile.getValue(), profile.getExtension(), profile::setValue)));
            // input[].codeFilter[].valueSet
            // output[].codeFilter[].valueSet
            inputOrOutput.getCodeFilter().stream()
                    .filter(DataRequirementCodeFilterComponent::hasValueSet)
                    .forEach(cf -> references.add(dependencyFromDataRequirementCodeFilter(cf)));
        });
        // action..definition
        var definition = action.getDefinition();
        if (definition != null && definition.hasReference()) {
            references.add(new DependencyInfo(
                    referenceSource, definition.getReference(), definition.getExtension(), definition::setReference));
        }
        action.getAction().forEach(nestedAction -> getDependenciesOfAction(nestedAction, references, referenceSource));
    }

    private DependencyInfo dependencyFromDataRequirementCodeFilter(DataRequirementCodeFilterComponent cf) {
        var vs = cf.getValueSet();
        if (vs instanceof StringType stringType) {
            return new DependencyInfo(
                    getPlanDefinition().getUrl(), stringType.getValue(), vs.getExtension(), stringType::setValue);
        } else if (vs instanceof Reference reference) {
            return new DependencyInfo(
                    getPlanDefinition().getUrl(), reference.getReference(), vs.getExtension(), reference::setReference);
        }
        return null;
    }

    @Override
    public Map<String, String> getReferencedLibraries() {
        var libraries = getPlanDefinition().getLibrary().stream()
                .collect(toMap(l -> requireNonNull(Canonicals.getTail(l.getReference())), Reference::getReference));
        libraries.putAll(resolveCqfLibraries());
        return libraries;
    }

    @Override
    public String getDescription() {
        return get().getDescription();
    }

    @Override
    public boolean hasLibrary() {
        return get().hasLibrary();
    }

    @Override
    public List<String> getLibrary() {
        return get().getLibrary().stream().map(Reference::getReference).collect(Collectors.toList());
    }

    @Override
    public boolean hasAction() {
        return get().hasAction();
    }

    @Override
    public List<IPlanDefinitionActionAdapter> getAction() {
        return get().getAction().stream().map(PlanDefinitionActionAdapter::new).collect(Collectors.toList());
    }
}
