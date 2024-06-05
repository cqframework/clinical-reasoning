package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.dstu3.model.StructureDefinition;
import org.hl7.fhir.dstu3.model.UriType;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.DependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;

public class StructureDefinitionAdapter extends KnowledgeArtifactAdapter {

    private StructureDefinition structureDefinition;

    public StructureDefinitionAdapter(StructureDefinition structureDefinition) {
        super(structureDefinition);
        this.structureDefinition = structureDefinition;
    }

    protected StructureDefinition getStructureDefinition() {
        return this.structureDefinition;
    }

    @Override
    public List<IDependencyInfo> getDependencies() {
        List<IDependencyInfo> references = new ArrayList<>();
        final String referenceSource = this.getStructureDefinition().hasVersion()
                ? this.getStructureDefinition().getUrl() + "|"
                        + this.getStructureDefinition().getVersion()
                : this.getStructureDefinition().getUrl();
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

        var libraryExtensions = structureDefinition.getExtensionsByUrl(Constants.CQF_LIBRARY);
        for (var libraryExt : libraryExtensions) {
            DependencyInfo dependency = new DependencyInfo(
                    referenceSource,
                    ((UriType) libraryExt.getValue()).asStringValue(),
                    libraryExt.getExtension(),
                    (reference) -> libraryExt.setValue(new UriType(reference)));
            references.add(dependency);
        }

        return references;
    }
}
