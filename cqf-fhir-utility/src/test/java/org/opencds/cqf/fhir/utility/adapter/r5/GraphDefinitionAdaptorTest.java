package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r5.model.GraphDefinition;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdaptorTest;
import java.util.Arrays;
import java.util.List;

public class GraphDefinitionAdaptorTest implements IGraphDefinitionAdaptorTest<GraphDefinition> {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();

    private final FhirContext fhirCtxt = FhirContext.forR5Cached();

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
        definition.getMeta().addProfile(information.ProfileRef);

        for (RelatedArtifactInfo info : information.RelatedArtifactInfo) {
            RelatedArtifact artifact = new RelatedArtifact();
            if (info.getRelatedArtifactType() == null) {
                artifact.setType(RelatedArtifactType.DEPENDSON);
            } else {
                artifact.setType((RelatedArtifactType) info.getRelatedArtifactType());
            }
            artifact.setResource(info.CanonicalResourceURL);

            definition.addExtension(info.ExtensionUrl, artifact);
        }

        return definition;
    }

    @Override
    public List<? extends Enum<?>> getAllNonProcessableTypeForRelatedArtifact() {
        return Arrays.stream(RelatedArtifactType.values())
            .filter(e -> e != RelatedArtifactType.DEPENDSON)
            .toList();
    }
}
