package org.opencds.cqf.fhir.utility.adapter.r4;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionConditionComponent;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionDynamicValueComponent;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;

public class PlanDefinitionAdapter extends KnowledgeArtifactAdapter implements IPlanDefinitionAdapter {

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
                .toList());

        // library[]
        List<CanonicalType> libraries = getPlanDefinition().getLibrary();
        for (CanonicalType ct : libraries) {
            DependencyInfo dependency =
                    new DependencyInfo(referenceSource, ct.getValue(), ct.getExtension(), ct::setValue);
            references.add(dependency);
        }
        // action[]
        getPlanDefinition().getAction().forEach(action -> getDependenciesOfAction(action, references, referenceSource));
        // extension[cpg-partOf]
        getPlanDefinition().getExtension().stream()
                .filter(ext -> ext.getUrl().contains("cpg-partOf"))
                .filter(Extension::hasValue)
                .findAny()
                .ifPresent(ext -> references.add(new DependencyInfo(
                        referenceSource,
                        ((CanonicalType) ext.getValue()).getValue(),
                        ext.getExtension(),
                        reference -> ext.setValue(new CanonicalType(reference)))));
        return references;
    }

    private void getDependenciesOfAction(
            PlanDefinition.PlanDefinitionActionComponent action,
            List<IDependencyInfo> references,
            String referenceSource) {
        action.getTrigger().stream().flatMap(t -> t.getData().stream()).forEach(eventData -> {
            // trigger[].dataRequirement[].profile[]
            eventData.getProfile().stream()
                    .filter(IPrimitiveType::hasValue)
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource, profile.getValue(), profile.getExtension(), profile::setValue)));
            // trigger[].dataRequirement[].codeFilter[].valueSet
            eventData.getCodeFilter().stream()
                    .filter(DataRequirementCodeFilterComponent::hasValueSet)
                    .forEach(cf -> references.add(
                            new DependencyInfo(referenceSource, cf.getValueSet(), cf.getExtension(), cf::setValueSet)));
        });
        // condition[].expression.reference
        action.getCondition().stream()
                .filter(PlanDefinitionActionConditionComponent::hasExpression)
                .map(PlanDefinitionActionConditionComponent::getExpression)
                .filter(Expression::hasReference)
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));
        // dynamicValue[].expression.reference
        action.getDynamicValue().stream()
                .filter(PlanDefinitionActionDynamicValueComponent::hasExpression)
                .map(PlanDefinitionActionDynamicValueComponent::getExpression)
                .filter(Expression::hasReference)
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        expression::setReference)));
        Stream.concat(action.getInput().stream(), action.getOutput().stream()).forEach(inputOrOutput -> {
            // ..input[].profile[]
            // ..output[].profile[]
            inputOrOutput.getProfile().stream()
                    .filter(IPrimitiveType::hasValue)
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource, profile.getValue(), profile.getExtension(), profile::setValue)));
            // input[].codeFilter[].valueSet
            // output[].codeFilter[].valueSet
            inputOrOutput.getCodeFilter().stream()
                    .filter(DataRequirementCodeFilterComponent::hasValueSet)
                    .forEach(cf -> references.add(
                            new DependencyInfo(referenceSource, cf.getValueSet(), cf.getExtension(), cf::setValueSet)));
        });
        // action..definitionCanonical
        var definition = action.getDefinitionCanonicalType();
        if (definition != null && definition.hasValue()) {
            references.add(new DependencyInfo(
                    referenceSource, definition.getValue(), definition.getExtension(), definition::setValue));
        }
        action.getAction().forEach(nestedAction -> getDependenciesOfAction(nestedAction, references, referenceSource));
    }

    @Override
    public Map<String, String> getReferencedLibraries() {
        var libraries = getPlanDefinition().getLibrary().stream()
                .collect(toMap(l -> requireNonNull(Canonicals.getIdPart(l)), CanonicalType::getValueAsString));
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
        return get().getLibrary().stream().map(PrimitiveType::asStringValue).collect(Collectors.toList());
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
