package org.opencds.cqf.fhir.utility.adapter.r5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.r5.model.GraphDefinition;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.RelatedArtifact;
import org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IDependencyInfo;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdaptorTest;

public class GraphDefinitionAdaptorTest implements IGraphDefinitionAdaptorTest<GraphDefinition> {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory =
            new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();

    private final FhirContext fhirCtxt = FhirContext.forR5Cached();

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
                .filter(e -> e != RelatedArtifactType.DEPENDSON && e != RelatedArtifactType.NULL)
                .map(d -> d.toCode())
                .toList();
    }

    @Test
    public void getDependencies_noReferenceButHasResourceRef_shouldWork() {
        // setup
        GraphDefinition definition = new GraphDefinition();
        definition.getMeta()
            .addProfile("profileref");
        String ref = "resourceRef";
        int count = 0;
        for (String url : new String[] { Constants.CPG_RELATED_ARTIFACT, Constants.ARTIFACT_RELATED_ARTIFACT }) {
            RelatedArtifact artifact = new RelatedArtifact();
            artifact.setType(RelatedArtifactType.DEPENDSON);
            artifact.setResourceReference(new Reference(ref + count++));
            definition.addExtension(url, artifact);
        }

        // test
        @SuppressWarnings("unchecked")
        IGraphDefinitionAdapter<GraphDefinition> adapter = getAdaptorFactory().createGraphDefinition(definition);
        List<IDependencyInfo> dependencies = adapter.getDependencies();

        // verify
        assertEquals(3, dependencies.size());
        assertTrue(dependencies.stream()
            .anyMatch(d -> d.getReference().equals("profileref")));
        for (int i = 0; i < 2; i++) {
            String val = ref + i;
            assertTrue(dependencies.stream()
                .anyMatch(d -> d.getReference().equals(val)));
        }
    }
}
