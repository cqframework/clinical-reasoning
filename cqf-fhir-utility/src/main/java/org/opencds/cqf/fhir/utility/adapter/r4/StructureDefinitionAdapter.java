package org.opencds.cqf.fhir.utility.adapter.r4;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.CanonicalType;
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
        final String referenceSource = getStructureDefinition().hasVersion()
                ? getStructureDefinition().getUrl() + "|"
                        + getStructureDefinition().getVersion()
                : getStructureDefinition().getUrl();
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

        var libraryExtensions = getStructureDefinition().getExtensionsByUrl(Constants.CQF_LIBRARY);
        for (var libraryExt : libraryExtensions) {
            DependencyInfo dependency = new DependencyInfo(
                    referenceSource,
                    ((CanonicalType) libraryExt.getValue()).asStringValue(),
                    libraryExt.getExtension(),
                    (reference) -> libraryExt.setValue(new CanonicalType(reference)));
            references.add(dependency);
        }

        return references;
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
