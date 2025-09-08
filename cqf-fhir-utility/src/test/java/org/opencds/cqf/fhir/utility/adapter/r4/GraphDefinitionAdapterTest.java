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

import java.util.Date;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.GraphDefinition;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.IGraphDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.TestVisitor;

class GraphDefinitionAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

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

        graphDef.addExtension(
                CPG_RELATED_ARTIFACT,
                new Expression().setReference(dependencies.get(1)).setExpression("someExp"));
        graphDef.addExtension(
                ARTIFACT_RELATED_ARTIFACT,
                new Expression().setReference(dependencies.get(2)).setExpression("someExp"));
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
}
