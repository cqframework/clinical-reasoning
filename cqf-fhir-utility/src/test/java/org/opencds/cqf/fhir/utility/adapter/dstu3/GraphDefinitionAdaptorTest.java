package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.dstu3.model.GraphDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.RelatedArtifact;
import org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdaptorTest;

import static org.junit.jupiter.api.Assertions.assertNull;

public class GraphDefinitionAdaptorTest implements IGraphDefinitionAdaptorTest<GraphDefinition> {

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

    @Test
    public void getReferenceFromArtifact_noResourceRef_coverageTest() {
        // setup
        RelatedArtifact artifact = new RelatedArtifact();
        GraphDefinition definition = new GraphDefinition();

        IGraphDefinitionAdapter adapter = getAdaptorFactory().createGraphDefinition(definition);

        // test
        String url = adapter.getReferenceFromArtifact(artifact);

        // verify
        assertNull(url);
    }

    @Test
    public void getReferenceFromArtifact_noArtifact_coverageTest() {
        // setup
        GraphDefinition definition = new GraphDefinition();

        IGraphDefinitionAdapter adapter = getAdaptorFactory().createGraphDefinition(definition);

        // test
        String url = adapter.getReferenceFromArtifact(new Reference("http://example.com"));

        // verify
        assertNull(url);
    }
}
