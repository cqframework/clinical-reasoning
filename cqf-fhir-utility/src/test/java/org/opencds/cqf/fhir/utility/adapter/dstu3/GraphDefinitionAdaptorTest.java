package org.opencds.cqf.fhir.utility.adapter.dstu3;

import org.hl7.fhir.dstu3.model.GraphDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdaptorTest;

public class GraphDefinitionAdaptorTest implements IGraphDefinitionAdaptorTest<GraphDefinition> {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();


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
            artifact.setType(RelatedArtifactType.DEPENDSON);
            artifact.setResource(new Reference(info.CanonicalResourceURL));

            definition.addExtension(info.ExtensionUrl, artifact);
        }


        return definition;
    }
}
