package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.dstu3.model.GraphDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdaptorTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphDefinitionAdaptorTest implements IGraphDefinitionAdaptorTest<GraphDefinition> {

    private static final Logger log = LoggerFactory.getLogger(GraphDefinitionAdaptorTest.class);
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory =
            new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();

    private final FhirContext fhirCtxt = FhirContext.forDstu3Cached();

    @Override
    public Class<GraphDefinition> graphDefinitionClass() {
        return GraphDefinition.class;
    }

    @Override
    public FhirContext fhirContext() {
        return fhirCtxt;
    }

    @Override
    public IAdapterFactory getAdaptorFactory() {
        return adapterFactory;
    }

    @Test
    public void test() {
        GraphDefinition definition = new GraphDefinition();
        RelatedArtifact artifact = new RelatedArtifact();
        artifact.setType(RelatedArtifactType.DEPENDSON);
        artifact.setResource(new Reference("canonical"));
        definition.addExtension(Constants.ARTIFACT_RELATED_ARTIFACT, artifact);

        System.out.println(fhirCtxt.newJsonParser().encodeToString(definition));
    }

    @Override
    public List<String> getAllNonProcessableTypeForRelatedArtifact() {
        return Arrays.stream(RelatedArtifactType.values())
                .filter(v -> v != RelatedArtifactType.DEPENDSON && v != RelatedArtifactType.NULL)
                .map(d -> d.toCode())
                .toList();
    }

    @Override
    public String toCanonicalReference(String ref) {
        // dstu3 does not set canonical references as urls directly,
        // but wraps them in a reference
        return String.format("{\"reference\":\"%s\"}", ref);
    }
}
