package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.GraphDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdaptorTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GraphDefinitionAdaptorTest implements IGraphDefinitionAdaptorTest<GraphDefinition> {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();

    private final FhirContext fhirCtxt = FhirContext.forDstu3Cached();

    @Override
    public FhirContext fhirContext() {
        return fhirCtxt;
    }

    @Override
    public IAdapterFactory getAdaptorFactory() {
        return adapterFactory;
    }

    @Override
    public GraphDefinition getGraphDefinition(GraphDefinitionInformation information) {
        GraphDefinition definition = new GraphDefinition();
        definition.getMeta()
            .addProfile(information.ProfileRef);

        for (RelatedArtifactInfo info : information.RelatedArtifactInfo) {
            RelatedArtifact artifact = new RelatedArtifact();
            if (info.getRelatedArtifactType() == null) {
                artifact.setType(RelatedArtifactType.DEPENDSON);
            } else {
                artifact.setType((RelatedArtifactType) info.getRelatedArtifactType());
            }
            artifact.setResource(new Reference(info.CanonicalResourceURL));

            definition.addExtension(info.ExtensionUrl, artifact);
        }


        return definition;
    }

    @Override
    public List<? extends Enum<?>> getAllNonProcessableTypeForRelatedArtifact() {
        return Arrays.stream(RelatedArtifactType.values())
            .filter(v -> v != RelatedArtifactType.DEPENDSON)
            .toList();
    }
}
