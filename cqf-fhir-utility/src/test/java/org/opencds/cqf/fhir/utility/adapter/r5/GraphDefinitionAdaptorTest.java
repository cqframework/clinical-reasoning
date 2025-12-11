package org.opencds.cqf.fhir.utility.adapter.r5;

import org.hl7.fhir.r5.model.GraphDefinition;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdaptorTest;

public class GraphDefinitionAdaptorTest implements IGraphDefinitionAdaptorTest<GraphDefinition> {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();


    @Override
    public IAdapterFactory getAdaptorFactory() {
        return adapterFactory;
    }

    @Override
    public GraphDefinition getGraphDefinition(GraphDefinitionInformation information) {
        GraphDefinition definition = new GraphDefinition();
        definition.getMeta().addProfile(information.ProfileRef);

        for (RelatedArtifactInfo info : information.RelatedArtifactInfo) {
            RelatedArtifact artifact = new RelatedArtifact();
            artifact.setType(RelatedArtifactType.DEPENDSON);
            artifact.setResource(info.CanonicalResourceURL);

            definition.addExtension(info.ExtensionUrl, artifact);
        }

        return definition;
    }
}
