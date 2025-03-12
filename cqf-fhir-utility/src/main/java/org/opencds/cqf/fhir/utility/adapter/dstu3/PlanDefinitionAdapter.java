package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.PlanDefinitionActionAdapter;

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
        references.addAll(getRelatedArtifact().stream()
                .map(ra -> DependencyInfo.convertRelatedArtifact(ra, referenceSource))
                .collect(Collectors.toList()));

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
        action.getTriggerDefinition().stream().map(t -> t.getEventData()).forEach(eventData -> {
            // trigger[].dataRequirement[].profile[]
            eventData.getProfile().stream()
                    .filter(UriType::hasValue)
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource, profile.getValue(), profile.getExtension(), profile::setValue)));
            // trigger[].dataRequirement[].codeFilter[].valueSet
            eventData.getCodeFilter().stream()
                    .filter(cf -> cf.hasValueSet())
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
                    .filter(cf -> cf.hasValueSet())
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
        if (vs instanceof StringType) {
            final var reference = (StringType) vs;
            return new DependencyInfo(
                    getPlanDefinition().getUrl(), reference.getValue(), vs.getExtension(), reference::setValue);
        } else if (vs instanceof Reference) {
            final var reference = (Reference) vs;
            return new DependencyInfo(
                    getPlanDefinition().getUrl(), reference.getReference(), vs.getExtension(), reference::setReference);
        }
        return null;
    }

    @Override
    public IBaseResource getPrimaryLibrary(Repository repository) {
        var libraries = getPlanDefinition().getLibrary();
        return libraries.isEmpty()
                ? null
                : SearchHelper.searchRepositoryByCanonical(
                        repository, libraries.get(0).getReferenceElement());
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
