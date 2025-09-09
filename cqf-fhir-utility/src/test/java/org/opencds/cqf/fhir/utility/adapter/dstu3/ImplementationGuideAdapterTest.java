package org.opencds.cqf.fhir.utility.adapter.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import java.util.List;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus;
import org.hl7.fhir.dstu3.model.ImplementationGuide;
import org.hl7.fhir.dstu3.model.ImplementationGuide.ImplementationGuidePackageComponent;
import org.hl7.fhir.dstu3.model.ImplementationGuide.ImplementationGuidePackageResourceComponent;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.StringType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.TestVisitor;

public class ImplementationGuideAdapterTest {
    private final org.opencds.cqf.fhir.utility.adapter.IAdapterFactory adapterFactory = new AdapterFactory();

    @Test
    void invalid_object_fails() {
        var planDefinition = new PlanDefinition();
        assertThrows(IllegalArgumentException.class, () -> new ImplementationGuideAdapter(planDefinition));
    }

    @Test
    void adapter_accepts_visitor() {
        var spyVisitor = spy(new TestVisitor());
        var implementationGuide = new ImplementationGuide();
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(implementationGuide);
        doReturn(implementationGuide).when(spyVisitor).visit(any(ImplementationGuideAdapter.class), any());
        adapter.accept(spyVisitor, null);
        verify(spyVisitor, times(1)).visit(any(ImplementationGuideAdapter.class), any());
    }

    @Test
    void adapter_get_and_set_name() {
        var implementationGuide = new ImplementationGuide();
        var name = "name";
        implementationGuide.setName(name);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(implementationGuide);
        assertEquals(name, adapter.getName());
        var newName = "name2";
        adapter.setName(newName);
        assertEquals(newName, implementationGuide.getName());
    }

    @Test
    void adapter_get_and_set_url() {
        var implementationGuide = new ImplementationGuide();
        var url = "www.url.com";
        implementationGuide.setUrl(url);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(implementationGuide);
        assertEquals(url, adapter.getUrl());
        var newUrl = "www.url2.com";
        adapter.setUrl(newUrl);
        assertEquals(newUrl, implementationGuide.getUrl());
    }

    @Test
    void adapter_get_and_set_version() {
        var implementationGuide = new ImplementationGuide();
        var version = "1.0.0";
        implementationGuide.setVersion(version);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(implementationGuide);
        assertTrue(adapter.hasVersion());
        assertEquals(version, adapter.getVersion());
        var newVersion = "1.0.1";
        adapter.setVersion(newVersion);
        assertEquals(newVersion, implementationGuide.getVersion());
    }

    @Test
    void adapter_get_and_set_status() {
        var implementationGuide = new ImplementationGuide();
        var status = PublicationStatus.DRAFT;
        implementationGuide.setStatus(status);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(implementationGuide);
        assertEquals(status.toCode(), adapter.getStatus());
        assertThrows(UnprocessableEntityException.class, () -> adapter.setStatus("invalid-status"));
        var newStatus = PublicationStatus.ACTIVE;
        adapter.setStatus(newStatus.toCode());
        assertEquals(newStatus, PublicationStatus.fromCode(adapter.getStatus()));
    }

    @Test
    void adapter_get_experimental() {
        var implementationGuide = new ImplementationGuide();
        var experimental = true;
        implementationGuide.setExperimental(experimental);
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(implementationGuide);
        assertEquals(experimental, adapter.getExperimental());
    }

    @Test
    void adapter_copy() {
        var implementationGuide = new ImplementationGuide().setStatus(PublicationStatus.DRAFT);
        implementationGuide.setId("implementationGuide-1");
        var adapter = adapterFactory.createKnowledgeArtifactAdapter(implementationGuide);
        var copy = (ImplementationGuide) adapter.copy();
        var adapterCopy = new ImplementationGuideAdapter(copy);
        adapterCopy.setId(new IdDt("ImplementationGuide", "implementationGuide-2"));
        assertNotEquals(implementationGuide.getId(), copy.getId());
        implementationGuide.setStatus(PublicationStatus.ACTIVE);
        assertNotEquals(adapter.getStatus(), copy.getStatus().toCode());
    }

    @Test
    void adapter_get_all_dependencies() {
        var dependencies = List.of("profileRef", "packageResourceRef");
        var ig = new ImplementationGuide();
        ig.getMeta().addProfile(dependencies.get(0));

        var igPackageResourceSource = new ImplementationGuidePackageResourceComponent(
                new BooleanType(false), new Reference(dependencies.get(1)));
        var igPackage = new ImplementationGuidePackageComponent(new StringType("exampleDependency"));
        igPackage.setResource(List.of(igPackageResourceSource));
        ig.setPackage(List.of(igPackage));

        // TODO: is the dependency element needed?

        var adapter = adapterFactory.createKnowledgeArtifactAdapter(ig);
        var extractedDependencies = adapter.getDependencies();
        assertEquals(extractedDependencies.size(), dependencies.size());
        extractedDependencies.forEach(dep -> {
            assertTrue(dependencies.contains(dep.getReference()));
        });
    }
}
