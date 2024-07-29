package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class StructureDefinitionAdapter extends KnowledgeArtifactAdapter {

    public StructureDefinitionAdapter(IDomainResource structureDefinition) {
        super(structureDefinition);
        if (!(structureDefinition instanceof StructureDefinition)) {
            throw new IllegalArgumentException(
                    "resource passed as planDefinition argument is not a StructureDefinition resource");
        }
    }

    public StructureDefinitionAdapter(StructureDefinition structureDefinition) {
        super(structureDefinition);
    }

    protected StructureDefinition getStructureDefinition() {
        return (StructureDefinition) resource;
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = getReferenceSource();
        addProfileReferences(references, referenceSource);

        /*
           extension[].url
           modifierExtension[].url
           baseDefinition
           differential.element[].type.code
           differential.element[].type.profile[]
           differential.element[].type.targetProfile[]
           differential.element[].binding.valueSet
           differential.element[].extension[].url
           differential.element[].modifierExtension[].url
           extension[cpg-inferenceExpression].reference
           extension[cpg-assertionExpression].reference
           extension[cpg-featureExpression].reference
        */

        if (get().hasBaseDefinition()) {
            references.add(new DependencyInfo(
                    referenceSource,
                    get().getBaseDefinition(),
                    get().getBaseDefinitionElement().getExtension(),
                    (reference) -> get().setBaseDefinition(reference)));
        }
        get().getExtensionsByUrl(Constants.CPG_ASSERTION_EXPRESSION).stream()
                .filter(e -> e.getValue() instanceof Expression)
                .map(e -> (Expression) e.getValue())
                .filter(e -> e.hasReference())
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        (reference) -> expression.setReference(reference))));
        get().getExtensionsByUrl(Constants.CPG_FEATURE_EXPRESSION).stream()
                .filter(e -> e.getValue() instanceof Expression)
                .map(e -> (Expression) e.getValue())
                .filter(e -> e.hasReference())
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        (reference) -> expression.setReference(reference))));
        get().getExtensionsByUrl(Constants.CPG_INFERENCE_EXPRESSION).stream()
                .filter(e -> e.getValue() instanceof Expression)
                .map(e -> (Expression) e.getValue())
                .filter(e -> e.hasReference())
                .forEach(expression -> references.add(new DependencyInfo(
                        referenceSource,
                        expression.getReference(),
                        expression.getExtension(),
                        (reference) -> expression.setReference(reference))));
        get().getDifferential()
                .getElement()
                .forEach(element -> getDependenciesOfDifferential(element, references, referenceSource));

        return references;
    }

    private void getDependenciesOfDifferential(
            ElementDefinition element, List<IDependencyInfo> references, String referenceSource) {
        element.getType().forEach(type -> {
            type.getProfile()
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource,
                            profile.getValueAsString(),
                            profile.getExtension(),
                            (reference) -> profile.setValue(reference))));
            type.getTargetProfile()
                    .forEach(profile -> references.add(new DependencyInfo(
                            referenceSource,
                            profile.getValueAsString(),
                            profile.getExtension(),
                            (reference) -> profile.setValue(reference))));
        });
        if (element.getBinding().hasValueSet()) {
            references.add(new DependencyInfo(
                    referenceSource,
                    element.getBinding().getValueSet(),
                    element.getBinding().getExtension(),
                    (reference) -> element.getBinding().setValueSet(reference)));
        }
    }

    @Override
    public StructureDefinition get() {
        return getStructureDefinition();
    }

    @Override
    public StructureDefinition copy() {
        return get().copy();
    }
}
