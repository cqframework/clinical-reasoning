package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.ElementDefinition;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.UriType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IElementDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IStructureDefinitionAdapter;

public class StructureDefinitionAdapter extends KnowledgeArtifactAdapter implements IStructureDefinitionAdapter {

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
                    reference -> get().setBaseDefinition(reference)));
        }

        get().getDifferential()
                .getElement()
                .forEach(element -> getDependenciesOfDifferential(element, references, referenceSource));

        return references;
    }

    private void getDependenciesOfDifferential(
            ElementDefinition element, List<IDependencyInfo> references, String referenceSource) {
        element.getType().forEach(type -> {
            if (type.hasProfile()) {
                references.add(new DependencyInfo(
                        referenceSource,
                        type.getProfile(),
                        type.getProfileElement().getExtension(),
                        type::setProfile));
            }
            if (type.hasTargetProfile()) {
                references.add(new DependencyInfo(
                        referenceSource,
                        type.getTargetProfile(),
                        type.getTargetProfileElement().getExtension(),
                        type::setTargetProfile));
            }
        });
        if (element.getBinding().hasValueSet()) {
            references.add(new DependencyInfo(
                    referenceSource,
                    element.getBinding().getValueSet().primitiveValue(),
                    element.getBinding().getExtension(),
                    reference -> element.getBinding().setValueSet(new UriType(reference))));
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

    @Override
    public List<IElementDefinitionAdapter> getSnapshotElements() {
        return get().getSnapshot().getElement().stream()
                .map(adapterFactory::createElementDefinition)
                .toList();
    }

    @Override
    public List<IElementDefinitionAdapter> getDifferentialElements() {
        return get().getDifferential().getElement().stream()
                .map(adapterFactory::createElementDefinition)
                .toList();
    }
}
