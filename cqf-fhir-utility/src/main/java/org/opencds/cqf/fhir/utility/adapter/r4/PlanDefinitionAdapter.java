package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class PlanDefinitionAdapter extends KnowledgeArtifactAdapter {

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
        final String referenceSource = getPlanDefinition().hasVersion()
                ? getPlanDefinition().getUrl() + "|" + getPlanDefinition().getVersion()
                : getPlanDefinition().getUrl();
        /*
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
        List<CanonicalType> libraries = getPlanDefinition().getLibrary();
        for (CanonicalType ct : libraries) {
            DependencyInfo dependency = new DependencyInfo(
                    referenceSource, ct.getValue(), ct.getExtension(), (reference) -> ct.setValue(reference));
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
                            ((CanonicalType) ext.getValue()).getValue(),
                            ext.getExtension(),
                            (reference) -> ext.setValue(new CanonicalType(reference))));
                });
        // TODO: Ideally use $data-requirements code

        return references;
    }

    private void getDependenciesOfAction(
            PlanDefinition.PlanDefinitionActionComponent action,
            List<IDependencyInfo> references,
            String referenceSource) {
        action.getTrigger().stream().flatMap(t -> t.getData().stream()).forEach(eventData -> {
            // trigger[].dataRequirement[].profile[]
            eventData.getProfile().stream()
                    .filter(profile -> profile.hasValue())
                    .forEach(profile -> {
                        references.add(new DependencyInfo(
                                referenceSource,
                                profile.getValue(),
                                profile.getExtension(),
                                (reference) -> profile.setValue(reference)));
                    });
            // trigger[].dataRequirement[].codeFilter[].valueSet
            eventData.getCodeFilter().stream().filter(cf -> cf.hasValueSet()).forEach(cf -> {
                references.add(new DependencyInfo(
                        referenceSource,
                        cf.getValueSet(),
                        cf.getExtension(),
                        (reference) -> cf.setValueSet(reference)));
            });
        });
        // condition[].expression.reference
        action.getCondition().stream()
                .filter(c -> c.hasExpression())
                .map(c -> c.getExpression())
                .filter(e -> e.hasReference())
                .forEach(expression -> {
                    references.add(new DependencyInfo(
                            referenceSource,
                            expression.getReference(),
                            expression.getExtension(),
                            (reference) -> expression.setReference(reference)));
                });
        // dynamicValue[].expression.reference
        action.getDynamicValue().stream()
                .filter(dv -> dv.hasExpression())
                .map(dv -> dv.getExpression())
                .filter(e -> e.hasReference())
                .forEach(expression -> {
                    references.add(new DependencyInfo(
                            referenceSource,
                            expression.getReference(),
                            expression.getExtension(),
                            (reference) -> expression.setReference(reference)));
                });
        Stream.concat(action.getInput().stream(), action.getOutput().stream()).forEach(inputOrOutput -> {
            // ..input[].profile[]
            // ..output[].profile[]
            inputOrOutput.getProfile().stream()
                    .filter(profile -> profile.hasValue())
                    .forEach(profile -> {
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
                        references.add(new DependencyInfo(
                                referenceSource,
                                cf.getValueSet(),
                                cf.getExtension(),
                                (reference) -> cf.setValueSet(reference)));
                    });
        });
        // action..definitionCanonical
        var definition = action.getDefinitionCanonicalType();
        if (definition != null && definition.hasValue()) {
            references.add(new DependencyInfo(
                    referenceSource,
                    definition.getValue(),
                    definition.getExtension(),
                    (reference) -> definition.setValue(reference)));
        }
        action.getAction().forEach(nestedAction -> getDependenciesOfAction(nestedAction, references, referenceSource));
    }
}
