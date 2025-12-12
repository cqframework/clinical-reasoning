package org.opencds.cqf.fhir.utility.adapter.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opencds.cqf.fhir.utility.Constants.ARTIFACT_RELATED_ARTIFACT;
import static org.opencds.cqf.fhir.utility.Constants.CPG_RELATED_ARTIFACT;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdaptorTest;
import org.opencds.cqf.fhir.utility.adapter.TestVisitor;

class GraphDefinitionAdapterTest implements IGraphDefinitionAdaptorTest<GraphDefinition> {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    private final FhirContext fhirCtxt = FhirContext.forR4Cached();

    @Test
    void invalid_object_fails() {
        var library = new Library();
        assertThrows(IllegalArgumentException.class, () -> adapterFactory.createGraphDefinition(library));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new TestVisitor());
        var graphDef = new GraphDefinition();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(graphDef);
        doReturn(graphDef).when(spyVisitor).visit(any(GraphDefinitionAdapter.class), any());

        adapter.accept(spyVisitor, null);
        verify(spyVisitor, times(1)).visit(any(GraphDefinitionAdapter.class), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var graphDef = new GraphDefinition();
        var name = "name";

        graphDef.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(graphDef);
        assertEquals(name, adapter.getName());

        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, graphDef.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var graphDef = new GraphDefinition();
        var url = "www.url.com";
        graphDef.setUrl(url);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(graphDef);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, graphDef.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var graphDef = new GraphDefinition();
        var version = "1.0.0";
        graphDef.setVersion(version);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(graphDef);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, graphDef.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var graphDef = new GraphDefinition();
        var status = PublicationStatus.DRAFT;
        graphDef.setStatus(status);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(graphDef);
        assertEquals(status.toCode(), adapter.getStatus());

        assertThrows(FHIRException.class, () -> adapter.setStatus("invalid-status"));

        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, graphDef.getStatus());
    }

    @Test
    void adapter_get_and_set_date() {
        var graphDef = new GraphDefinition();
        var date = new Date();

        graphDef.setDate(date);

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(graphDef);
        assertEquals(date, adapter.getDate());

        var newDate = new Date();
        newDate.setTime(100);
        adapter.setDate(newDate);
        assertEquals(newDate, graphDef.getDate());
    }

    @Test
    void adapter_get_and_set_dependencies() {
        var dependencies = List.of("profileRef", "someRef0", "someRef1");

        var graphDef = new GraphDefinition();
        graphDef.getMeta().addProfile(dependencies.get(0));

        RelatedArtifact cpgArtifact = new RelatedArtifact();
        cpgArtifact.setType(RelatedArtifactType.DEPENDSON);
        cpgArtifact.setResource(dependencies.get(1));

        graphDef.addExtension(
                CPG_RELATED_ARTIFACT,
                cpgArtifact);

        RelatedArtifact artifact = new RelatedArtifact();
        artifact.setType(RelatedArtifactType.DEPENDSON);
        artifact.setResource(dependencies.get(2));
        graphDef.addExtension(ARTIFACT_RELATED_ARTIFACT, artifact);

        // we shouldn't find this one
        graphDef.addExtension(
                "someURL", new Expression().setReference("someRef").setExpression("someExp"));

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(graphDef);

        var extractedDependencies = adapter.getDependencies();

        assertEquals(extractedDependencies.size(), dependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.contains(dep.getReference()));
        });
    }

    @Test
    void adapter_get_backBoneElements() {
        var graphDef = new GraphDefinition();

        graphDef.addLink().setDescription("Link1");
        graphDef.addLink().setDescription("Link2");

        var adapter = (IGraphDefinitionAdapter) adapterFactory.createKnowledgeArtifactAdapter(graphDef);

        assertEquals(2, adapter.getBackBoneElements().size());
    }

    @Test
    void adapter_get_node() {
        var graphDef = new GraphDefinition();

        var adapter = (IGraphDefinitionAdapter) adapterFactory.createKnowledgeArtifactAdapter(graphDef);

        assertEquals(0, adapter.getNode().size());
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
